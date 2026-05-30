package lhs.finalproject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class DepthPuller {

    public static final String PYTHON_EXECUTABLE = "/Users/deepacganesan/image_Depth/env/bin/python3";
    public static final String PYTHON_SCRIPT     = "/Users/deepacganesan/image_Depth/main.py";

    public static final float COIN_REAL_DIAMETER_CM = 2.426f;
    public static final float FALLBACK_FOCAL_PX = 2740.0f;

    // ---- Persistent Python worker (loaded once, reused for all images) ----
    private static Process worker;
    private static BufferedWriter workerIn;
    private static BufferedReader workerOut;

    /** Starts the DA3 worker once. Safe to call repeatedly; only the first call starts it. */
    public static synchronized void startWorker() throws IOException {
        if (worker != null && worker.isAlive()) return;

        ProcessBuilder pb = new ProcessBuilder(PYTHON_EXECUTABLE, PYTHON_SCRIPT);
        pb.redirectErrorStream(true);
        worker = pb.start();
        workerIn  = new BufferedWriter(new OutputStreamWriter(worker.getOutputStream()));
        workerOut = new BufferedReader(new InputStreamReader(worker.getInputStream()));

        // Wait for the model to finish loading ("Waiting for jobs...").
        String line;
        while ((line = workerOut.readLine()) != null) {
            System.out.println("    [PY] " + line);
            if (line.contains("Waiting for jobs")) break;
        }
    }

    /** Cleanly stops the worker. Call once at the end of the whole pipeline. */
    public static synchronized void stopWorker() {
        try {
            if (workerIn != null) {
                workerIn.write("QUIT\n");
                workerIn.flush();
            }
            if (worker != null) worker.waitFor();
        } catch (Exception ignored) {
        } finally {
            worker = null; workerIn = null; workerOut = null;
        }
    }

    // ---- Diagnostics from the most recent run() ----
    public float focalPx, deskDepthTrueCm, bgMedianDA3, scaleK, maxHeightCm;

    /** Output holder: calibrated height map + depth-corrected object dimensions (cm). */
    public static class Result {
        public final float[][] heightMap;
        public final double[] dimsCm;   // {widthCm, heightCm} of the main object
        public Result(float[][] heightMap, double[] dimsCm) {
            this.heightMap = heightMap;
            this.dimsCm = dimsCm;
        }
    }

    public Result run(String jpegPath, String heicPath, BufferedImage cannyMask, double ppi, int coinCutoffFullResX)
            throws IOException, InterruptedException {

        String binPath = jpegPath + ".depth.bin";
        float[][] rawDepth = runDA3(jpegPath, binPath);
        int dh = rawDepth.length, dw = rawDepth[0].length;

        boolean[][] isObject = scaleMaskToObjectFlags(cannyMask, dw, dh, coinCutoffFullResX);

        // Desk plane depth (median over background)
        ArrayList<Float> bgDepths = new ArrayList<>();
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++)
                if (!isObject[y][x]) bgDepths.add(rawDepth[y][x]);
        if (bgDepths.isEmpty())
            throw new IOException("No background pixels in mask for " + jpegPath);
        Collections.sort(bgDepths);
        bgMedianDA3 = bgDepths.get(bgDepths.size() / 2);

        // Option 1 calibration
        focalPx = resolveFocalPx(heicPath, cannyMask.getWidth(), cannyMask.getHeight());
        deskDepthTrueCm = (float) (focalPx / ppi);
        float bgMedianCm = bgMedianDA3 * 100.0f;
        if (bgMedianCm <= 0f) throw new IOException("DA3 desk depth non-positive for " + jpegPath);
        scaleK = deskDepthTrueCm / bgMedianCm;

        // Height map in cm
        float[][] heightMap = new float[dh][dw];
        maxHeightCm = 0f;
        float mean = 0f; int n = dh * dw;
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                if (!isObject[y][x]) {
                    heightMap[y][x] = 0f;
                } else {
                    float h = scaleK * (bgMedianDA3 - rawDepth[y][x]) * 100.0f;
                    if (h < 0f) h = 0f;
                    heightMap[y][x] = h;
                    if (h > maxHeightCm) maxHeightCm = h;
                }
                mean += heightMap[y][x];
            }
        }
        mean /= n;
        float variance = 0f;
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++)
                variance += (heightMap[y][x] - mean) * (heightMap[y][x] - mean);
        variance /= n;

        // ---- Problem 3 fix: dimensions at the OBJECT's own depth plane, not the coin's ----
        double[] dimsCm = correctedDimensionsCm(rawDepth, isObject, ppi);

        System.out.printf("    [DepthPuller] DA3 %dx%d | focal=%.1f | PPI=%.2f | k=%.4f | maxH=%.2fcm | dims=%.2f x %.2f cm%n",
                dw, dh, focalPx, ppi, scaleK, maxHeightCm, dimsCm[0], dimsCm[1]);
        if (variance < 1e-4f)
            System.out.println("    [DepthPuller][WARN] Height map nearly flat - DA3 may be degenerate.");
        if (scaleK <= 0f || Float.isNaN(scaleK) || Float.isInfinite(scaleK))
            throw new IOException("Bad scale k=" + scaleK + " for " + jpegPath);

        return new Result(heightMap, dimsCm);
    }

    /**
     * Problem 3: object width/height measured at the object's median depth, not the coin's.
     * PPI is valid at the desk plane; pixels-per-cm scales inversely with depth, so the
     * object (closer to camera) has MORE px/cm. We correct by the depth ratio.
     */
    private double[] correctedDimensionsCm(float[][] rawDepth, boolean[][] isObject, double deskPpi) {
        int dh = rawDepth.length, dw = rawDepth[0].length;
        int minX = dw, maxX = -1, minY = dh, maxY = -1;
        ArrayList<Float> objDepths = new ArrayList<>();
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                if (isObject[y][x]) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                    objDepths.add(rawDepth[y][x]);
                }
            }
        }
        if (maxX < 0 || objDepths.isEmpty()) return new double[]{0, 0};
        Collections.sort(objDepths);
        float objMedianDepth = objDepths.get(objDepths.size() / 2);

        // deskPpi was measured on the FULL-RES mask; convert bbox px (DA3 res) to full-res px.
        double resScale = (double) cannyFullW / dw;   // see note below
        double objPxW = (maxX - minX) * resScale;
        double objPxH = (maxY - minY) * resScale;

        // px/cm at object plane = deskPpi * (deskDepth / objectDepth)
        double objPpi = deskPpi * (bgMedianDA3 / objMedianDepth);
        return new double[]{ objPxW / objPpi, objPxH / objPpi };
    }

    // set by scaleMaskToObjectFlags so correctedDimensionsCm knows the full-res width
    private int cannyFullW = 1;

    private boolean[][] scaleMaskToObjectFlags(BufferedImage mask, int dw, int dh, int coinCutoffFullResX) {
        cannyFullW = mask.getWidth();
        BufferedImage scaled = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(mask, 0, 0, dw, dh, null);
        g.dispose();

        int cutoffDA3 = (coinCutoffFullResX < 0) ? dw : (int)((long) coinCutoffFullResX * dw / cannyFullW);

        boolean[][] isObject = new boolean[dh][dw];
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++)
                isObject[y][x] = x <= cutoffDA3 && (scaled.getRGB(x, y) & 0xFFFFFF) < 0x808080;
        return isObject;
    }

    /** Sends one job to the persistent worker and waits for JOB_DONE. */
    public float[][] runDA3(String jpegPath, String binPath) throws IOException, InterruptedException {
        startWorker();
        workerIn.write(jpegPath + "\t" + binPath + "\n");
        workerIn.flush();

        String line;
        while ((line = workerOut.readLine()) != null) {
            if (line.equals("JOB_DONE")) break;
            if (line.equals("JOB_FAILED")) throw new IOException("DA3 worker failed on " + jpegPath);
            System.out.println("    [PY] " + line);
        }
        return readRawDepthBinary(binPath);
    }

    public static float[][] readRawDepthBinary(String binPath) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(binPath)))) {
            int height = Integer.reverseBytes(dis.readInt());
            int width  = Integer.reverseBytes(dis.readInt());
            if (height <= 0 || width <= 0 || height > 20000 || width > 20000)
                throw new IOException("Implausible depth dims " + width + "x" + height);
            float[][] depth = new float[height][width];
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    depth[y][x] = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
            return depth;
        }
    }

    public float resolveFocalPx(String heicPath, int workW, int workH) {
        if (heicPath == null) return FALLBACK_FOCAL_PX;
        try {
            float fxRef = Float.NaN, refLong = Float.NaN;

            ProcessBuilder pb1 = new ProcessBuilder("exiftool", "-s3", "-IntrinsicMatrix", heicPath);
            pb1.redirectErrorStream(true);
            Process p1 = pb1.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p1.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    for (String t : line.trim().split("[,\\s]+")) {
                        try { fxRef = Float.parseFloat(t); break; } catch (NumberFormatException ignore) {}
                    }
                    if (!Float.isNaN(fxRef)) break;
                }
            }
            p1.waitFor();

            ProcessBuilder pb2 = new ProcessBuilder("exiftool", "-s3", "-IntrinsicMatrixReferenceDimensions", heicPath);
            pb2.redirectErrorStream(true);
            Process p2 = pb2.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    float a = Float.NaN, b = Float.NaN;
                    for (String t : line.trim().split("[,\\sx]+")) {
                        try {
                            float v = Float.parseFloat(t);
                            if (Float.isNaN(a)) a = v; else { b = v; break; }
                        } catch (NumberFormatException ignore) {}
                    }
                    if (!Float.isNaN(a) && !Float.isNaN(b)) refLong = Math.max(a, b);
                }
            }
            p2.waitFor();

            if (Float.isNaN(fxRef)) return FALLBACK_FOCAL_PX;
            if (Float.isNaN(refLong)) return fxRef;
            return fxRef * (Math.max(workW, workH) / refLong);
        } catch (Exception e) {
            return FALLBACK_FOCAL_PX;
        }
    }
}