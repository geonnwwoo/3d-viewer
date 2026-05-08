package lhs.finalproject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class CannyEdgeDetector {
    double sigma;
    double highThresholdRatio;
    double lowThresholdRatio;
    float strong = 255.0f;
    float weak = 100.0f;
    float blank = 0.0f;

    public CannyEdgeDetector(double sigma, double highThresholdRatio, double lowThresholdRatio) {
        this.sigma = sigma;
        this.highThresholdRatio = highThresholdRatio;
        this.lowThresholdRatio = lowThresholdRatio;
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
        int kernelSize = 5;
        float sum = 0;
        float[][] kernalNotNormalized = new float[kernelSize][kernelSize];
        float[][] kernal = new float[kernelSize][kernelSize];

        for(int r = 0; r<kernelSize; r++){
            for(int c= 0; c < kernelSize; c++){
                int x = r-2;
                int y = c-2;
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

        for (int y = 2; y<height - 2; y++){
            for (int x = 2; x<width -2; x++){
                float blurredPixelValue = 0.0f;
                for (int i = 0; i<kernelSize; i++){
                    for (int j = 0; j < kernelSize; j++){
                        float surrounding = grayscale[y + i -2][x + j -2];
                        blurredPixelValue += surrounding * kernal[i][j];
                    }
                }
                blurred[y][x] = blurredPixelValue;
            }
        }

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (y<2 || y >= height -2 || x < 2 || x >= width -2){
                    blurred[y][x] = grayscale[y][x];
                }
            }
        }

        return blurred;
    }

    public float[][] computeGradientMagnitude(float[][] blurred) {
        int[][] Gx = {{-1,0,1},
                      {-2,0,2},
                      {-1,0,1}};

        int[][] Gy = {{-1,-2,-1},
                      {0,0,0},
                      {1,2,1}};

        int height = blurred.length;
        int width = blurred[0].length;
        float[][] gradient = new float[height][width];

        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
                float gradientSumX = 0.0f;
                float gradientSumY = 0.0f;
                for (int i = 0; i<3; i++){
                    for (int j = 0; j < 3; j++){
                        float surrounding = blurred[y + i -1][x + j -1];
                        gradientSumX += surrounding * Gx[i][j];
                        gradientSumY += surrounding * Gy[i][j];
                    }
                }
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
        int[][] Gx = {{-1,0,1},
                {-2,0,2},
                {-1,0,1}};

        int[][] Gy = {{-1,-2,-1},
                {0,0,0},
                {1,2,1}};

        int height = blurred.length;
        int width = blurred[0].length;
        float[][] gradient = new float[height][width];

        for (int y = 1; y<height - 1; y++){
            for (int x = 1; x<width -1; x++){
                float gradientSumX = 0.0f;
                float gradientSumY = 0.0f;
                for (int i = 0; i<3; i++){
                    for (int j = 0; j < 3; j++){
                        float surrounding = blurred[y + i -1][x + j -1];
                        gradientSumX += surrounding * Gx[i][j];
                        gradientSumY += surrounding * Gy[i][j];
                    }
                }
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
                    for (int offsetY = -2; offsetY <=2; offsetY++) {
                        for (int offsetX = -2; offsetX <= 2; offsetX++) {
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
                if (x < width * 0.1 || x > width*0.9 || y < height * 0.1 || y > height * 0.9){
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
        float maxMag = 0.0f;
        int height = inputMatrix.length;
        int width = inputMatrix[0].length;
        for (int y = 0; y<height; y++) {
            for (int x = 0; x < width; x++) {
                float val = inputMatrix[y][x];
                if (val > maxMag){
                    maxMag = val;
                }
            }
        }

        int[] histogram = new int[(int)(maxMag+1)];

        for (int y = 0; y<height; y++) {
            for (int x = 0; x < width; x++) {
                int val = (int) inputMatrix[y][x];
                histogram[val] ++;
            }
        }

        float maxVar = 0.0f;
        int bestLow = 0;
        int bestHigh = 0;
        int totalPixels = height*width;

        for(int h = 1; h<=maxMag;h++){
            for(int l = 0; l < h; l++){
                int count1 =0;
                int count2 = 0;
                int count3 = 0;
                float sum1 = 0.0f;
                float sum2 = 0.0f;
                float sum3 = 0.0f;

                for(int i = 0; i <= l; i++){
                    count1 += histogram[i];
                    sum1 += i*histogram[i];
                }
                for(int i = l+1; i <= h; i++){
                    count2 += histogram[i];
                    sum2 += i*histogram[i];
                }
                for(int i = h+1; i <= (int)maxMag; i++){
                    count3 += histogram[i];
                    sum3 += i*histogram[i];
                }

                float w1 = (float) count1/totalPixels;
                float w2 = (float) count2/totalPixels;
                float w3 = (float) count3/totalPixels;

                float p1;
                if (count1 ==0){
                    p1 = 0.0f;
                }
                else{
                    p1 = sum1/count1;
                }
                float p2;
                if (count2 ==0){
                    p2 = 0.0f;
                }
                else{
                    p2 = sum2/count2;
                }
                float p3;
                if (count3 ==0){
                    p3 = 0.0f;
                }
                else{
                    p3 = sum3/count3;
                }

                float mean = (p1*w1) + (p2 * w2) + (p3 * w3);
                float var = (w1 * (p1 - mean) * (p1-mean)) + (w2 * (p2 - mean) * (p2-mean)) + (w3 * (p3 - mean) * (p3-mean));
                if (var > maxVar){
                    maxVar = var;
                    bestLow = l;
                    bestHigh = h;
                }
            }
        }

        return new float[]{bestLow, bestHigh};

    }



//    public float[][] interpolate(float[][] chopped){
//
//    }

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
        float[][] grayscale = toGrayscale(image);
        float[][] blurred = applyGaussianBlur(grayscale);
        float[][] magnitude = computeGradientMagnitude(blurred);
        float[][] angle = computeGradientAngle(blurred);
        float[][] suppressed = applyNonMaxSuppression(magnitude, angle);
        float[][] threshold = applyDoubleThreshold(suppressed);
        float[][] almostThere = applyHysteresis(threshold);
        float[][] chopped = chop(almostThere);
//        float[][] interpolated = interpolate(chopped);
//        float[][] finalized = lineThickener(chopped);
        return toBufferedImage(chopped);

    }


    public static void main(String[] args) {
        try {
            // 1. Load your test image
            File inputFile = new File("src/main/resources/test_images/IMG_1278.jpg"); // <-- Change this to your file name
            BufferedImage inputImage = ImageIO.read(inputFile);

            // 2. Initialize your detector (Sigma, High Threshold, Low Threshold)
            CannyEdgeDetector detector = new CannyEdgeDetector(2.0, 55.0, 25.0);

            // 3. Run the full detection pipeline
            System.out.println("Processing image...");
            BufferedImage result = detector.detect(inputImage);

            // 4. Save the final result to see your hard work
            File outputFile = new File("src/main/resources/results_of_tests/mom.png");
            ImageIO.write(result, "png", outputFile);

            System.out.println("Done! Check canny_output.png in your project folder.");

        } catch (IOException e) {
            System.out.println("Error: Could not find or read the image file.");
            e.printStackTrace();
        }
    }





}



