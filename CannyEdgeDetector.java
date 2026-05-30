package lhs.finalproject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Queue;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CannyEdgeDetector {
    public double sigma;
    public double highThresholdRatio;
    public double lowThresholdRatio;
    public float strong = 255.0f;
    public float weak = 100.0f;
    public float blank = 0.0f;

    // Z-score cutoff for the color background mask. Tune: lower if object pixels
    // are being eaten as background, raise if boundary/shadow reads as object.
    public float colorZThreshold = 4.0f;

    public final int[][] Gx = {
            {-1,0,1},
            {-2,0,2},
            {-1,0,1}
    };

    public final int[][] Gy = {
            {-1,-2,-1},
            {0,0,0},
            {1,2,1}
    };

    public CannyEdgeDetector(double sigma, double highThresholdRatio, double lowThresholdRatio) {
        this.sigma = sigma;
        this.highThresholdRatio = highThresholdRatio;
        this.lowThresholdRatio = lowThresholdRatio;
    }

    public CannyEdgeDetector(){
        this.sigma = 2.2;
        this.highThresholdRatio = 140.0;
        this.lowThresholdRatio = 60.0;
    }

    public float[][] toGrayscale(BufferedImage image) {
        BufferedImage grey = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grey.getGraphics();
        g.drawImage(image, 0 ,0, null);
        g.dispose();

        int height = grey.getHeight();
        int width = grey.getWidth();

        float[][] result = new float[height][width];

        for (int y = 0; y < height; y++){
            for (int x= 0; x < width; x++){
                int pixelValue = grey.getRaster().getSample(x, y, 0);
                result[y][x] = pixelValue;
            }
        }

        return result;
    }

    public float[][] applyGaussianBlur(float[][] grayscale) {

        int kernelSize = 2 * (int) (3 * sigma) + 1;
        int halfSize = kernelSize / 2;
        float sum = 0;
        float[][] kernalNotNormalized = new float[kernelSize][kernelSize];
        float[][] kernal = new float[kernelSize][kernelSize];

        for(int r = 0; r<kernelSize; r++){
            for(int c= 0; c < kernelSize; c++){
                int x = r - halfSize;
                int y = c - halfSize;
                double exponent = - (Math.pow(x, 2) + Math.pow(y, 2))/(2 * Math.pow(sigma,2));
                double multiplier = 1/(2 * Math.PI * Math.pow(sigma, 2));
                float val = (float)(multiplier * Math.exp(exponent));
                kernalNotNormalized[r][c] = val;
                sum += val;
            }
        }

        for(int r = 0; r<kernelSize; r++) {
            for (int c = 0; c < kernelSize; c++) {
                float val = kernalNotNormalized[r][c]/sum;
                kernal[r][c] = val;
            }
        }

        int height = grayscale.length;
        int width = grayscale[0].length;
        float[][] blurred = new float[height][width];

        for (int y = halfSize; y<height - halfSize; y++){
            for (int x = halfSize; x<width -halfSize; x++){
                float blurredPixelValue = 0.0f;
                for (int i = 0; i<kernelSize; i++){
                    for (int j = 0; j < kernelSize; j++){
                        float surrounding = grayscale[y + i -halfSize][x + j -halfSize];
                        blurredPixelValue += surrounding * kernal[i][j];
                    }
                }
                blurred[y][x] = blurredPixelValue;
            }
        }

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (y < halfSize || y >= height - halfSize || x < halfSize || x >= width - halfSize){
                    blurred[y][x] = grayscale[y][x];
                }
            }
        }

        return blurred;
    }

    public float[][] computeGradientMagnitude(float[][] blurred) {
//        int[][] Gx = {{-1,0,1},
//                {-2,0,2},
//                {-1,0,1}};
//
//        int[][] Gy = {{-1,-2,-1},
//                {0,0,0},
//                {1,2,1}};
//
        int height = blurred.length;
        int width = blurred[0].length;
        float[][] gradient = new float[height][width];
//
        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
//                float gradientSumX = 0.0f;
//                float gradientSumY = 0.0f;
//                for (int i = 0; i<3; i++){
//                    for (int j = 0; j < 3; j++){
//                        float surrounding = blurred[y + i -1][x + j -1];
//                        gradientSumX += surrounding * Gx[i][j];
//                        gradientSumY += surrounding * Gy[i][j];
//                    }
//                }
                float[] sobel = computeSobel(blurred, y, x);
                float gradientSumX = sobel[0];
                float gradientSumY = sobel[1];
                gradient[y][x] = (float) Math.sqrt(gradientSumX * gradientSumX + gradientSumY * gradientSumY);
            }
        }


        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (y<1 || y >= height -1 || x < 1 || x >= width -1){
                    gradient[y][x] = 0;
                }
            }
        }

        return gradient;
    }

    public float[][] computeGradientAngle(float[][] blurred) {
//        int[][] Gx = {{-1,0,1},
//                {-2,0,2},
//                {-1,0,1}};
//
//        int[][] Gy = {{-1,-2,-1},
//                {0,0,0},
//                {1,2,1}};

        int height = blurred.length;
        int width = blurred[0].length;
        float[][] gradient = new float[height][width];

        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
//                float gradientSumX = 0.0f;
//                float gradientSumY = 0.0f;
//                for (int i = 0; i<3; i++){
//                    for (int j = 0; j < 3; j++){
//                        float surrounding = blurred[y + i -1][x + j -1];
//                        gradientSumX += surrounding * Gx[i][j];
//                        gradientSumY += surrounding * Gy[i][j];
//                    }
//                }
                float[] sobel = computeSobel(blurred, y, x);
                float gradientSumX = sobel[0];
                float gradientSumY = sobel[1];

                float angle = (float) Math.atan2(gradientSumY, gradientSumX);
                float angleDegrees = (float) Math.toDegrees(angle);
                if (angleDegrees < 0){
                    angleDegrees += 180;
                }
                gradient[y][x] = angleDegrees;
            }
        }

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (y<1 || y >= height -1 || x < 1 || x >= width -1){
                    gradient[y][x] = 0;
                }
            }
        }

        return gradient;

    }

    public float[] computeSobel(float[][] blurred, int y, int x) {

        float gradientSumX = 0.0f;
        float gradientSumY = 0.0f;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {

                float surrounding = blurred[y + i - 1][x + j - 1];

                gradientSumX += surrounding * Gx[i][j];
                gradientSumY += surrounding * Gy[i][j];
            }
        }

        return new float[]{gradientSumX, gradientSumY};
    }

    public float[][] applyNonMaxSuppression(float[][] magnitude, float[][] angle) {
        int height = magnitude.length;
        int width = magnitude[0].length;
        float [][] result = new float[height][width];
        for (int y = 1; y < height -1; y++){
            for (int x = 1; x < width - 1; x++){
                float currMag = magnitude[y][x];
                float currAng = angle[y][x];
                float neighbor1, neighbor2;

                if ((currAng >= 0 && currAng < 22.5) || (currAng>= 157.5 && currAng <= 180)){
                    neighbor1 = magnitude[y][x+1];
                    neighbor2 = magnitude[y][x-1];
                }
                else if (currAng >= 22.5 && currAng < 67.5){
                    neighbor1 = magnitude[y-1][x-1];
                    neighbor2 = magnitude[y+1][x+1];
                }
                else if (currAng >= 67.5 && currAng < 112.5){
                    neighbor1 = magnitude[y-1][x];
                    neighbor2 = magnitude[y+1][x];
                }
                else{
                    neighbor1 = magnitude[y-1][x+1];
                    neighbor2 = magnitude[y+1][x-1];
                }


                if((currMag >= neighbor1) && (currMag >= neighbor2)){
                    result[y][x] = currMag;
                }
                else{
                    result[y][x] = 0.0f;
                }

            }
        }
        return result;
    }

    public float[][] applyDoubleThreshold(float[][] suppressed) {
        int height = suppressed.length;
        int width = suppressed[0].length;

        float[][] threshold = new float[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float magnitude = suppressed[y][x];
                if (magnitude >= highThresholdRatio){
                    threshold[y][x] = strong;
                }
                else if (magnitude >= lowThresholdRatio) {
                    threshold[y][x] = weak;
                }
                else{
                    threshold[y][x] = blank;
                }
            }
        }
        return threshold;
    }

    // This has the at least one advanced data structure (stack) due to dfs :)
    public float[][] applyHysteresis(float[][] thresholded) {
        Stack<int[]> strongPixels = new Stack<>(); // <-- right here, Yippee!
        int height = thresholded.length;
        int width = thresholded[0].length;

        float[][] finalEdges = new float[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                finalEdges[i][j] = 0.0f;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (thresholded[y][x] == strong){
                    strongPixels.add(new int[]{y,x});
                    finalEdges[y][x] = strong;
                }
            }
        }

        while(!strongPixels.isEmpty()){
            int[] current = strongPixels.pop();
            int currY = current[0];
            int currX = current[1];
            for (int offsetY = -1; offsetY <=1; offsetY++){
                for(int offsetX = -1; offsetX <= 1; offsetX++){
                    int neighborY = currY + offsetY;
                    int neighborX = currX + offsetX;
                    if ((neighborX >= 0 && neighborX < width) && (neighborY >= 0 && neighborY < height)){
                        if(finalEdges[neighborY][neighborX] != strong){
                            if(thresholded[neighborY][neighborX] == weak){
                                finalEdges[neighborY][neighborX] = strong;
                                strongPixels.push(new int[] {neighborY, neighborX});
                            }
                        }
                    }
                }
            }
        }

        return finalEdges;
    }

    // ORIGINAL thickener (radius hard-coded to 6). Kept for reference / thought process.
    // Superseded by the parameterized lineThickener(finalEdges, radius) below, which
    // detect() now calls with a thin radius so the color mask carries the silhouette.
    public float[][] lineThickener(float[][] finalEdges){
        int height = finalEdges.length;
        int width = finalEdges[0].length;
        float[][] thickened = new float[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                thickened[i][j] = 0.0f;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (finalEdges[y][x] == strong){
                    for (int offsetY = -6; offsetY <=6; offsetY++) {
                        for (int offsetX = -6; offsetX <= 6; offsetX++) {
                            int neighborY = y + offsetY;
                            int neighborX = x + offsetX;
                            if ((neighborX >= 0 && neighborX < width) && (neighborY >= 0 && neighborY < height)) {
                                thickened[neighborY][neighborX] = strong;
                            }
                        }
                    }
                }
            }
        }

        return thickened;
    }

    // NEW: parameterized thickener. With color fusion we only need a THIN wall
    // (radius ~2) to stop the soft anti-aliased boundary from leaking inward,
    // since the color mask carries the silhouette. A fat wall just eats the edge.
    public float[][] lineThickener(float[][] finalEdges, int radius){
        int height = finalEdges.length;
        int width = finalEdges[0].length;
        float[][] thickened = new float[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (finalEdges[y][x] == strong){
                    for (int offsetY = -radius; offsetY <= radius; offsetY++) {
                        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                            int neighborY = y + offsetY;
                            int neighborX = x + offsetX;
                            if ((neighborX >= 0 && neighborX < width) && (neighborY >= 0 && neighborY < height)) {
                                thickened[neighborY][neighborX] = strong;
                            }
                        }
                    }
                }
            }
        }

        return thickened;
    }

    public float[][] chop(float[][] finalEdges){
        int height = finalEdges.length;
        int width = finalEdges[0].length;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < width * 0.05 || x > width*0.95 || y < height * 0.05 || y > height * 0.95){
                    finalEdges[y][x] = 0;
                }
            }
        }
        return finalEdges;
    }

    public float estimateNoise(float[][] grayscale){
        int[][] lapKernel = {{0,1,0},
                {1,-4,1},
                {0,1,0}};

        int height = grayscale.length;
        int width = grayscale[0].length;
        float[][] lapacian = new float[height][width];
        float globalSum = 0.0f;

        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
                float sum = 0.0f;
                for (int i = 0; i<3; i++){
                    for (int j = 0; j < 3; j++){
                        float surrounding = grayscale[y + i -1][x + j -1];
                        sum += surrounding * lapKernel[i][j];
                    }
                }
                lapacian[y][x] = sum;
                globalSum += sum;
            }
        }

        float mean = globalSum/((height-2) * (width-2));
        float estimatedSigma = 0.0f;

        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
                estimatedSigma += (lapacian[y][x] - mean)*(lapacian[y][x] - mean);
            }
        }
        float var = estimatedSigma/((height-2) * (width-2));
        float minVar = 100.0f;
        float maxVar = 1000.f;
        float minSigma = 0.5f;
        float maxSigma = 3.0f;
        float clamped = Math.clamp(var, minVar, maxVar);
        float sigma = minSigma + ((clamped - minVar)*(maxSigma - minSigma))/(maxVar - minVar);
        return sigma;
    }
//I tried to implement something i found called Otsu's method which gives us better value of the high and low threshold
//paper can be found at this link: https://pmc.ncbi.nlm.nih.gov/articles/PMC11836388/

    float[] adaptiveThresholds(float[][] inputMatrix){
        int height = inputMatrix.length;
        int width = inputMatrix[0].length;
        float maxMag = 0.0f;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (inputMatrix[y][x] > maxMag) {
                    maxMag = inputMatrix[y][x];
                }
        int range = (int)(maxMag + 1);
        int[] histogram = new int[range];
        for(int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[(int) inputMatrix[y][x]]++;
            }
        }

        long[] prefixCount = new long[range + 1];
        long[] prefixSum   = new long[range + 1];
        for (int i = 0; i < range; i++) {
            prefixCount[i+1] = prefixCount[i] + histogram[i];
            prefixSum[i+1]   = prefixSum[i] + (long) i*histogram[i];
        }

        int totalPixels = height * width;
        float maxVar = 0.0f;
        int bestLow = 0, bestHigh = 0;

        for (int h = 1; h < range; h++) {
            for (int l = 0; l < h; l++) {
                long c1 = prefixCount[l+1];
                long c2 = prefixCount[h+1] - prefixCount[l+1];
                long c3 = prefixCount[range] -prefixCount[h+1];
                long s1 = prefixSum[l+1];
                long s2 = prefixSum[h+1] - prefixSum[l+1];
                long s3 = prefixSum[range]- prefixSum[h+1];

                float w1 = (float) c1 / totalPixels;
                float w2 = (float) c2 / totalPixels;
                float w3 = (float) c3 / totalPixels;
                float p1;
                if (c1 == 0) {
                    p1 = 0.0f;
                } else {
                    p1 = (float) s1 / c1;
                }

                float p2;
                if (c2 == 0) {
                    p2 = 0.0f;
                } else {
                    p2 = (float) s2 / c2;
                }

                float p3;
                if (c3 == 0) {
                    p3 = 0.0f;
                } else {
                    p3 = (float) s3 / c3;
                }

                float mean = w1*p1 + w2*p2 + w3*p3;
                float var  = w1*(p1-mean)*(p1-mean) + w2*(p2-mean)*(p2-mean) + w3*(p3-mean)*(p3-mean);
                if (var > maxVar) {
                    maxVar = var;
                    bestLow = l;
                    bestHigh = h;
                }
            }
        }
        return new float[]{bestLow, bestHigh};
    }


    // ============================================================
    //  COLOR FUSION: learn the mat color, build a hard color mask,
    //  and flood fill gated by (color-is-mat AND not-an-edge).
    //
    //  v1 (kept in PI notes): modeled the mat in RGB and z-scored
    //  Euclidean distance. PROBLEM: RGB couples brightness into the
    //  color, so a shadow on the mat (dark murky pink) sits a huge
    //  Euclidean distance from bright pink and got flagged as object
    //  -> shadow blob welded onto the silhouette.
    //
    //  v2 (this version): model the mat in HSB and measure distance
    //  on HUE + SATURATION only, ignoring BRIGHTNESS. A shadow keeps
    //  its hue and (on a neon mat) most of its saturation, so it stays
    //  background. Saturation also does the heavy lifting for rejecting
    //  the OBJECT: white/gray/black object pixels have near-zero
    //  saturation -> far from the vivid mat -> flagged object, even
    //  though their hue is meaningless noise.
    //
    //  Two extra subtleties handled here:
    //    - Hue is circular and neon pink (hue ~0.91) is near the 1.0
    //      wrap, so the mean hue is a CIRCULAR mean (average unit
    //      vectors, atan2 back) not a plain average, and hue distance
    //      wraps at 0.5.
    //    - Variance uses the deviation-from-mean form, not
    //      (sumSq/n - mean^2), so there is no catastrophic-cancellation
    //      risk on large n (also moot now that H,S are in [0,1]).
    // ============================================================

    /** HSB model of the mat: circular-mean hue + mean saturation, with their std devs. */
    public static class BackgroundModel {
        public final float mh, ms;   // mean hue (circular), mean saturation
        public final float sh, ss;   // std dev of hue (circular), std dev of saturation
        public BackgroundModel(float mh, float ms, float sh, float ss) {
            this.mh = mh; this.ms = ms;
            this.sh = sh; this.ss = ss;
        }
    }

    // RETAINED from v1 (RGB model): std-dev from running sums. No longer called now
    // that variance is computed in deviation form inside fit(); kept for reference.
    private float std(double sumSq, double sum, long n) {
        double var = sumSq / n - (sum / n) * (sum / n);
        if (var < 0) var = 0;
        return Math.max(1.0f, (float) Math.sqrt(var));
    }

    /**
     * Z-distance of a pixel from the mat model, in HSB on hue+saturation only.
     * Brightness is deliberately excluded so shadows do not inflate the distance.
     * hsbBuf is a reused length-3 scratch array (avoids a per-pixel allocation).
     */
    private float zdist(int r, int g, int b, BackgroundModel m, float[] hsbBuf) {
        Color.RGBtoHSB(r, g, b, hsbBuf);
        float h = hsbBuf[0];   // hue 0..1
        float s = hsbBuf[1];   // saturation 0..1
        // brightness hsbBuf[2] intentionally ignored

        float dh = Math.abs(h - m.mh);
        if (dh > 0.5f) dh = 1.0f - dh;   // circular: 0.0 and 1.0 are the same hue
        dh /= m.sh;

        float ds = (s - m.ms) / m.ss;

        return (float) Math.sqrt(dh * dh + ds * ds);
    }

    /**
     * Fits an HSB mat model over the rough-background pixels. If `gate` is non-null,
     * only pixels within 2.5z of `gate` are used (the robust outlier-trim pass that
     * drops object pixels which leaked in through a Canny contour gap).
     * Circular mean for hue; deviation-form variance for numerical stability.
     */
    private BackgroundModel fit(BufferedImage img, boolean[][] roughBg,
                                BackgroundModel gate, float[] buf) {
        int height = roughBg.length, width = roughBg[0].length;

        // --- Pass A: circular mean of hue + mean of saturation ---
        double sumCos = 0, sumSin = 0, sumS = 0; long n = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (!roughBg[y][x]) continue;
            int rgb = img.getRGB(x, y);
            int r = (rgb>>16)&0xFF, g = (rgb>>8)&0xFF, b = rgb&0xFF;
            if (gate != null && zdist(r, g, b, gate, buf) > 2.5f) continue;
            Color.RGBtoHSB(r, g, b, buf);
            double ang = 2.0 * Math.PI * buf[0];
            sumCos += Math.cos(ang);
            sumSin += Math.sin(ang);
            sumS   += buf[1];
            n++;
        }
        if (n == 0) return new BackgroundModel(0.91f, 0.85f, 0.05f, 0.12f); // neon-pink fallback
        float meanH = (float) (Math.atan2(sumSin, sumCos) / (2.0 * Math.PI));
        if (meanH < 0) meanH += 1.0f;
        float meanS = (float) (sumS / n);

        // --- Pass B: deviation-form variance (stable), hue distance wraps ---
        double varH = 0, varS = 0; long n2 = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (!roughBg[y][x]) continue;
            int rgb = img.getRGB(x, y);
            int r = (rgb>>16)&0xFF, g = (rgb>>8)&0xFF, b = rgb&0xFF;
            if (gate != null && zdist(r, g, b, gate, buf) > 2.5f) continue;
            Color.RGBtoHSB(r, g, b, buf);
            float dh = Math.abs(buf[0] - meanH);
            if (dh > 0.5f) dh = 1.0f - dh;
            float ds = buf[1] - meanS;
            varH += dh * dh;
            varS += ds * ds;
            n2++;
        }
        float sdH = Math.max(0.01f, (float) Math.sqrt(varH / n2));  // floors avoid div-by-zero
        float sdS = Math.max(0.02f, (float) Math.sqrt(varS / n2));
        return new BackgroundModel(meanH, meanS, sdH, sdS);
    }

    /**
     * Learns the mat distribution from the rough (Canny-only) background.
     * Two-pass: fit once, then refit trimming everything beyond 2.5z of the first
     * fit. Sampling the whole flood-filled region (not just corners) also dodges
     * the corner-poisoning problem when an object touches a corner.
     */
    public BackgroundModel learnBackground(BufferedImage original, boolean[][] roughBackground) {
        float[] buf = new float[3];                      // single reused HSB scratch
        BackgroundModel rough = fit(original, roughBackground, null, buf);
        return fit(original, roughBackground, rough, buf);
    }

    /** Hard binary color mask: true where the pixel is within zThreshold of the mat (HSB). */
    public boolean[][] colorBackgroundMask(BufferedImage original, BackgroundModel m, float zThreshold) {
        int height = original.getHeight(), width = original.getWidth();
        float[] buf = new float[3];                      // single reused HSB scratch
        boolean[][] isBg = new boolean[height][width];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int rgb = original.getRGB(x, y);
            int r = (rgb>>16)&0xFF, g = (rgb>>8)&0xFF, b = rgb&0xFF;
            isBg[y][x] = zdist(r, g, b, m, buf) <= zThreshold;
        }
        return isBg;
    }

    private void addSeed(int y, int x, float[][] edges, boolean[][] colorBg,
                         boolean[][] background, Queue<int[]> queue) {
        if (!background[y][x] && edges[y][x] != strong && colorBg[y][x]) {
            background[y][x] = true;
            queue.add(new int[]{y, x});
        }
    }

    /**
     * Fused flood fill: a pixel is reachable background only if it is mat-colored
     * AND not a strong edge. This is the marriage of the two methods.
     */
    public boolean[][] floodFillFused(float[][] edges, boolean[][] colorBg) {
        int height = edges.length, width = edges[0].length;
        boolean[][] background = new boolean[height][width];
        Queue<int[]> queue = new LinkedList<>();

        for (int x=0; x<width; x++) {
            addSeed(0, x, edges, colorBg, background, queue);
            addSeed(height-1, x, edges, colorBg, background, queue);
        }
        for (int y=0; y<height; y++) {
            addSeed(y, 0, edges, colorBg, background, queue);
            addSeed(y, width-1, edges, colorBg, background, queue);
        }

        int[] dy={-1,1,0,0}, dx={0,0,-1,1};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int d=0; d<4; d++) {
                int ny=c[0]+dy[d], nx=c[1]+dx[d];
                if (ny>=0 && ny<height && nx>=0 && nx<width
                        && !background[ny][nx]
                        && edges[ny][nx] != strong
                        && colorBg[ny][nx]) {
                    background[ny][nx] = true;
                    queue.add(new int[]{ny, nx});
                }
            }
        }
        return background;
    }


    // ORIGINAL flood fill (Canny-only). Still used as PASS 1 in detect() to get a
    // rough background for sampling the mat color, then floodFillFused does pass 2.
    public boolean[][] floodFillFromCorners(float[][] edges) {
        int height = edges.length;
        int width = edges[0].length;
        boolean[][] background = new boolean[height][width];
        Queue<int[]> queue = new LinkedList<>();

        for (int x = 0; x < width; x++) {
            if (!background[0][x] && edges[0][x] != strong) { background[0][x] = true; queue.add(new int[]{0, x}); }
            if (!background[height-1][x] && edges[height-1][x] != strong) { background[height-1][x] = true; queue.add(new int[]{height-1, x}); }
        }
        for (int y = 0; y < height; y++) {
            if (!background[y][0] && edges[y][0] != strong) { background[y][0] = true; queue.add(new int[]{y, 0}); }
            if (!background[y][width-1] && edges[y][width-1] != strong) { background[y][width-1] = true; queue.add(new int[]{y, width-1}); }
        }

        int[] dy = {-1, 1, 0, 0};
        int[] dx = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            for (int d = 0; d < 4; d++) {
                int ny = curr[0] + dy[d];
                int nx = curr[1] + dx[d];
                if (ny >= 0 && ny < height && nx >= 0 && nx < width
                        && !background[ny][nx] && edges[ny][nx] != strong) {
                    background[ny][nx] = true;
                    queue.add(new int[]{ny, nx});
                }
            }
        }
        return background;
    }


    // Morphological open (dilate then erode) on the edge map. Was written as an
    // alternative denoise step; never wired into detect(). Kept for thought process.
    public float[][] denoise(float[][] edges) {
        int height = edges.length;
        int width = edges[0].length;

        float[][] dilated = new float[height][width];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float max = 0.0f;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        max = Math.max(max, edges[y + dy][x + dx]);
                    }
                }
                dilated[y][x] = max;
            }
        }


        float[][] eroded = new float[height][width];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float min = Float.MAX_VALUE;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        min = Math.min(min, dilated[y + dy][x + dx]);
                    }
                }
                eroded[y][x] = min;
            }
        }

        return eroded;
    }

    public BufferedImage toBufferedImage(float[][] img) {
        int height = img.length;
        int width = img[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float pixel = img[y][x];
                if (pixel >= 0){
                    int pixelInt = Math.min(255, Math.round(pixel));
                    int rgb = (pixelInt <<16) | (pixelInt << 8) | pixelInt;
                    image.setRGB(x, y, rgb);
                }
            }
        }
        return image;
    }

    public BufferedImage detect(BufferedImage image) {
        float[][] grayscale  = toGrayscale(image);
        this.sigma = estimateNoise(grayscale);
        float[][] blurred    = applyGaussianBlur(grayscale);
        float[][] magnitude  = computeGradientMagnitude(blurred);
        float[][] angle      = computeGradientAngle(blurred);
        float[] thresholds       = adaptiveThresholds(magnitude);
        this.lowThresholdRatio   = thresholds[0];
        this.highThresholdRatio  = thresholds[1];
        float[][] suppressed = applyNonMaxSuppression(magnitude, angle);
        float[][] threshold  = applyDoubleThreshold(suppressed);
        float[][] edges      = applyHysteresis(threshold);
        float[][] chopped    = chop(edges);

        // OLD single-pass path (kept for reference):
        //   float[][] thickened  = lineThickener(chopped);     // radius 6
        //   float[][] rechopped  = chop(thickened);
        //   boolean[][] background = floodFillFromCorners(rechopped);

        // NEW two-pass color-fusion path:
        // Thin wall now: color carries the silhouette, the edge only crispens the boundary.
        float[][] wall       = lineThickener(chopped, 2);
        float[][] rechopped  = chop(wall);

        // Pass 1: rough background from Canny alone, used only to sample the mat color.
        boolean[][] roughBg = floodFillFromCorners(rechopped);

        // Learn the pink mat distribution (HSB, shadow-proof) from those rough-bg pixels.
        BackgroundModel model = learnBackground(image, roughBg);

        // Pass 2: hard color mask, then fused flood fill (mat-colored AND not an edge wall).
        boolean[][] colorBg    = colorBackgroundMask(image, model, colorZThreshold);
        boolean[][] background = floodFillFused(rechopped, colorBg);

        int height = edges.length;
        int width  = edges[0].length;
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (background[y][x]) {
                    mask.setRGB(x, y, 0xFFFFFF);
                } else {
                    mask.setRGB(x, y, 0x000000);
                }
            }
        }
        return mask;
    }

//    public static void main(String[] args) {
//        try {
//            File inputFile = new File("src/main/resources/test_images/IMG_1663.jpg");
//            BufferedImage inputImage = ImageIO.read(inputFile);
//
//            CannyEdgeDetector detector = new CannyEdgeDetector();
//
//            System.out.println("Processing image...");
//            long start = System.currentTimeMillis();
//            BufferedImage result = detector.detect(inputImage);
//            long end = System.currentTimeMillis();
//            System.out.println("Pipeline took: " + (end - start) + "ms");
//
//            File outputFile = new File("src/main/resources/results_of_tests/IMG_1663.png");
//            ImageIO.write(result, "png", outputFile);
//
//            System.out.println("Done! Check canny_output.png in your project folder.");
//
//        } catch (IOException e) {
//            System.out.println("Error: Could not find or read the image file.");
//            e.printStackTrace();
//        }
//    }



}