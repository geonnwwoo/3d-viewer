package lhs.finalproject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class MainRunner {

    public final float voxelSizeCm = 0.1f;
    public final String[] directions = {"Front", "Back", "Left", "Right", "Top", "Bottom"};
    public File scanFolder;

    public MainRunner() {
        this.scanFolder = getLatestScanFolder();
    }

    public float[][][] runPipeline() throws IOException, InterruptedException {
        System.out.println("Processing scan: " + scanFolder.getName());

        float[][][] heightMaps = new float[6][][];
        double[][] objectDimsCm = new double[6][];

        for (int i = 0; i < directions.length; i++) {
            String currDir = directions[i];
            System.out.println("\n======= " + currDir + " =======");

            File jpegFile = new File(scanFolder, currDir + ".jpeg");
            File heicFile = new File(scanFolder, currDir + ".HEIC");

            rotateIfNeeded(jpegFile);

            BufferedImage jpeg = ImageIO.read(jpegFile);
            if (jpeg == null) {
                throw new IOException("Could not load: " + jpegFile.getPath());
            }

            CannyEdgeDetector canny = new CannyEdgeDetector();
            BufferedImage cannyMask = canny.detect(jpeg);

            ImageIO.write(cannyMask, "png", new File(scanFolder, currDir + "_canny.png"));
            System.out.println("Canny mask saved.");

            // Coin-based lateral calibration: PPI at the desk plane.
            image_dimensions dims = new image_dimensions();
            int[][] arr = dims.toArray(cannyMask);
            dims.pixelSize(arr);
            System.out.printf("PPI: %.2f px/cm%n", dims.PPI);

            // Erase the coin from the mask so DepthPuller never sees it, regardless of
            // where the coin sits (top/bottom/left/right). image_dimensions found the
            // coin by circularity and exposed its bbox; we paint the background colour
            // (white) over that box + a small buffer. Guard: only erase if a coin was
            // actually found (coinMinX >= 0); otherwise the -1 sentinel would paint a
            // garbage rectangle.
            if (dims.coinMinX >= 0) {
                int buf = 5;
                Graphics2D g2d = cannyMask.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(
                        dims.coinMinX - buf,
                        dims.coinMinY - buf,
                        (dims.coinMaxX - dims.coinMinX) + 2 * buf,
                        (dims.coinMaxY - dims.coinMinY) + 2 * buf
                );
                g2d.dispose();
                // Save the erased mask so you can visually verify the coin is gone.
                ImageIO.write(cannyMask, "png", new File(scanFolder, currDir + "_canny_erased.png"));
            } else {
                System.out.println("    [MainRunner][WARN] No coin found for " + currDir
                        + " - PPI is unreliable and nothing was erased.");
            }

            // DA3 depth + Option 1 coin/focal calibration. Returns the height map (cm)
            // AND depth-corrected object dimensions (Problem 3): measured at the object's
            // own depth plane instead of the coin's plane.
            // Pass -1 for the cutoff: the coin is physically gone from the mask now, so
            // DepthPuller needs no positional cropping.
            String heicPath = heicFile.exists() ? heicFile.getPath() : null;
            DepthPuller depth = new DepthPuller();
            DepthPuller.Result res = depth.run(jpegFile.getPath(), heicPath, cannyMask, dims.PPI, -1);

            heightMaps[i] = res.heightMap;
            objectDimsCm[i] = res.dimsCm;
            System.out.printf("Object: %.2f cm wide x %.2f cm tall%n", objectDimsCm[i][0], objectDimsCm[i][1]);
            System.out.printf("Height map: %dx%d px (max height %.2f cm).%n",
                    heightMaps[i][0].length, heightMaps[i].length, depth.maxHeightCm);
        }

        System.out.println("\n======= Building voxel grid =======");
        float[][][] voxelGrid = buildVoxelGrid(heightMaps, objectDimsCm);

        System.out.println("\nDone. Voxel grid ready for downstream processing.");
        return voxelGrid;
    }

    public float[][][] buildVoxelGrid(float[][][] heightMaps, double[][] objectDimsCm) {
        double objX = max4(objectDimsCm[0][0], objectDimsCm[1][0], objectDimsCm[4][0], objectDimsCm[5][0]);
        double objY = max4(objectDimsCm[0][1], objectDimsCm[1][1], objectDimsCm[2][1], objectDimsCm[3][1]);
        double objZ = max4(objectDimsCm[2][0], objectDimsCm[3][0], objectDimsCm[4][1], objectDimsCm[5][1]);

        int gridX = Math.max(1, (int)(objX / voxelSizeCm));
        int gridY = Math.max(1, (int)(objY / voxelSizeCm));
        int gridZ = Math.max(1, (int)(objZ / voxelSizeCm));

        System.out.printf("Physical object size: %.2f x %.2f x %.2f cm%n", objX, objY, objZ);
        System.out.printf("Voxel grid: %d(X) x %d(Y) x %d(Z) = %,d voxels%n", gridX, gridY, gridZ, (long) gridX * gridY * gridZ);

        // Seatbelt: if a bad calibration slips through and inflates a dimension, fail
        // loudly with a readable message instead of a raw OutOfMemoryError mid-allocation.
        long voxelCount = (long) gridX * gridY * gridZ;
        if (voxelCount > 200_000_000L) {
            throw new RuntimeException("Voxel grid too large (" + voxelCount + " voxels). "
                    + "A view's PPI is almost certainly wrong - check the per-view dims above "
                    + "and the *_canny.png masks.");
        }

        boolean[][][] solid = new boolean[gridZ][gridY][gridX];
        float[][][] grid = new float[gridZ][gridY][gridX];

        for (int z = 0; z < gridZ; z++) {
            for (int y = 0; y < gridY; y++) {
                for (int x = 0; x < gridX; x++) {
                    solid[z][y][x] = true;
                }
            }
        }

        stampLeft(heightMaps[0], solid, grid, gridX, gridY, gridZ);
        stampRight(heightMaps[1], solid, grid, gridX, gridY, gridZ);
        stampBack(heightMaps[2], solid, grid, gridX, gridY, gridZ);
        stampFront(heightMaps[3], solid, grid, gridX, gridY, gridZ);
        stampTop(heightMaps[4], solid, grid, gridX, gridY, gridZ);
        stampBottom(heightMaps[5], solid, grid, gridX, gridY, gridZ);

        for (int gz = 0; gz < gridZ; gz++) {
            for (int gy = 0; gy < gridY; gy++) {
                for (int gx = 0; gx < gridX; gx++) {
                    if (!solid[gz][gy][gx]) {
                        grid[gz][gy][gx] = 0f;
                    } else if (grid[gz][gy][gx] == 0f) {
                        grid[gz][gy][gx] = 1f;
                    }
                }
            }
        }

        return grid;
    }

    private int[] objectBounds(float[][] heightMap) {
        int h = heightMap.length, w = heightMap[0].length;
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (heightMap[y][x] > 0f) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
        if (maxX < 0) return new int[]{0, 0, w - 1, h - 1}; // fallback: whole image
        return new int[]{minX, minY, maxX, maxY};
    }
    public void stampFront(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gx = clamp((int)((float)(px - minX) / bw * gridX), 0, gridX - 1);
                int gy = clamp((int)((float)(py - minY) / bh * gridY), 0, gridY - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                for (int gz = depthV; gz < gridZ; gz++) solid[gz][gy][gx] = false;
                if (depthV > 0 && depthV <= gridZ)
                    grid[depthV - 1][gy][gx] = Math.max(grid[depthV - 1][gy][gx], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Front");
    }

    public void stampBack(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gx = clamp(gridX - 1 - (int)((float)(px - minX) / bw * gridX), 0, gridX - 1);
                int gy = clamp((int)((float)(py - minY) / bh * gridY), 0, gridY - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                int objectStartZ = gridZ - depthV;
                for (int gz = 0; gz < objectStartZ; gz++) solid[gz][gy][gx] = false;
                if (objectStartZ >= 0 && objectStartZ < gridZ)
                    grid[objectStartZ][gy][gx] = Math.max(grid[objectStartZ][gy][gx], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Back");
    }

    public void stampLeft(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gz = clamp((int)((float)(px - minX) / bw * gridZ), 0, gridZ - 1);
                int gy = clamp((int)((float)(py - minY) / bh * gridY), 0, gridY - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                for (int gx = depthV; gx < gridX; gx++) solid[gz][gy][gx] = false;
                if (depthV > 0 && depthV <= gridX)
                    grid[gz][gy][depthV - 1] = Math.max(grid[gz][gy][depthV - 1], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Left");
    }

    public void stampRight(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gz = clamp(gridZ - 1 - (int)((float)(px - minX) / bw * gridZ), 0, gridZ - 1);
                int gy = clamp((int)((float)(py - minY) / bh * gridY), 0, gridY - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                int objectStartX = gridX - depthV;
                for (int gx = 0; gx < objectStartX; gx++) solid[gz][gy][gx] = false;
                if (objectStartX >= 0 && objectStartX < gridX)
                    grid[gz][gy][objectStartX] = Math.max(grid[gz][gy][objectStartX], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Right");
    }

    public void stampTop(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gx = clamp((int)((float)(px - minX) / bw * gridX), 0, gridX - 1);
                int gz = clamp((int)((float)(py - minY) / bh * gridZ), 0, gridZ - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                for (int gy = depthV; gy < gridY; gy++) solid[gz][gy][gx] = false;
                if (depthV > 0 && depthV <= gridY)
                    grid[gz][depthV - 1][gx] = Math.max(grid[gz][depthV - 1][gx], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Top");
    }

    public void stampBottom(float[][] heightMap, boolean[][][] solid, float[][][] grid, int gridX, int gridY, int gridZ) {
        int[] b = objectBounds(heightMap);
        int minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
        int bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                int gx = clamp((int)((float)(px - minX) / bw * gridX), 0, gridX - 1);
                int gz = clamp(gridZ - 1 - (int)((float)(py - minY) / bh * gridZ), 0, gridZ - 1);
                int depthV = (int)(heightMap[py][px] / voxelSizeCm);
                int objectStartY = gridY - depthV;
                for (int gy = 0; gy < objectStartY; gy++) solid[gz][gy][gx] = false;
                if (objectStartY >= 0 && objectStartY < gridY)
                    grid[gz][objectStartY][gx] = Math.max(grid[gz][objectStartY][gx], heightMap[py][px]);
            }
        }
        System.out.println("Stamped: Bottom");
    }

    public File getLatestScanFolder() {
        File downloads = new File(System.getProperty("user.home") + "/Downloads");
        File[] allFiles = downloads.listFiles();

        File latest = null;
        long maxTs = -1;

        if (allFiles != null) {
            for (int i = 0; i < allFiles.length; i++) {
                File f = allFiles[i];
                if (f.isDirectory() && f.getName().startsWith("scan_")) {
                    long ts = Long.parseLong(f.getName().replace("scan_", ""));
                    if (ts > maxTs) {
                        maxTs = ts;
                        latest = f;
                    }
                }
            }
        }

        if (latest == null) {
            throw new RuntimeException("No scan_ folders found in ~/Downloads");
        }

        System.out.println("Found: " + latest.getAbsolutePath());
        return latest;
    }

    public void rotateIfNeeded(File imageFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("exiftool", "-Orientation", "-n", imageFile.getPath());
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;
        int orientation = 1;
        while ((line = reader.readLine()) != null) {
            if (line.contains("Orientation")) {
                try {
                    orientation = Integer.parseInt(line.split(":")[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        proc.waitFor();

        if (orientation == 1) return;

        int degrees = 0;
        if (orientation == 6) {
            degrees = 90;
        } else if (orientation == 3) {
            degrees = 180;
        } else if (orientation == 8) {
            degrees = 270;
        }

        if (degrees == 0) return;

        BufferedImage rotated = rotateImage(ImageIO.read(imageFile), degrees);
        ImageIO.write(rotated, "jpeg", imageFile);
        System.out.println("Rotated " + degrees + "°: " + imageFile.getName());
    }

    public BufferedImage rotateImage(BufferedImage src, int degrees) {
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (degrees == 90 || degrees == 270) ? h : w;
        int newH = (degrees == 90 || degrees == 270) ? w : h;

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.translate(newW / 2.0, newH / 2.0);
        g.rotate(Math.toRadians(degrees));
        g.translate(-w / 2.0, -h / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();

        return out;
    }


    public double max4(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    public int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

//    public static void main(String[] args) {
//        try {
//            MainRunner runner = new MainRunner();
//            runner.runPipeline();
//            DepthPuller.stopWorker();   // cleanly stop the persistent DA3 worker
//            System.exit(0);
//        } catch (IOException | InterruptedException e) {
//            System.out.println("Error: Pipeline failed.");
//            e.printStackTrace();
//            DepthPuller.stopWorker();
//            System.exit(1);
//        }
//    }
}