package lhs.finalproject;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;

public class image_dimensions {
    public final double refSize = 2.426;
    public double PPI;
    public int mainEdge = -1;
    private double objectWidth, objectHeight;

    // Helper class to store data about our black shapes (blobs)
    class Blob {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = -1, maxY = -1;
        int pixelCount = 0;
        int perimeter = 0;   // # of pixel edges touching background / image border

        // Circularity = 4*pi*area / perimeter^2. A filled disc -> ~1.0 (a bit under,
        // due to staircased perimeter); an elongated object -> well below 1.
        // Used only to RANK blobs by roundness, so the absolute value doesn't matter.
        double circularity() {
            if (perimeter <= 0) return 0;
            return 4.0 * Math.PI * pixelCount / ((double) perimeter * perimeter);
        }
    }

    public int[][] toArray(BufferedImage bnwPhoto) {
        int width = bnwPhoto.getWidth();
        int height = bnwPhoto.getHeight();
        int[][] result = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = bnwPhoto.getRGB(x, y);
            }
        }
        return result;
    }

    public void pixelSize(int[][] imageArray) {
        int height = imageArray.length;
        int width = imageArray[0].length;
        int black = -16777216; // Pure black
        long totalPixels = (long) height * width;

        boolean[][] visited = new boolean[height][width];
        ArrayList<Blob> blobs = new ArrayList<>();

        // DFS to find all blobs of black pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imageArray[y][x] == black && !visited[y][x]) {
                    Blob blob = new Blob();
                    Stack<int[]> stack = new Stack<>();
                    stack.push(new int[]{y, x});
                    visited[y][x] = true;

                    while (!stack.isEmpty()) {
                        int[] curr = stack.pop();
                        int cy = curr[0], cx = curr[1];
                        blob.pixelCount++;

                        // Track the bounding box of this specific blob
                        if (cx < blob.minX) blob.minX = cx;
                        if (cx > blob.maxX) blob.maxX = cx;
                        if (cy < blob.minY) blob.minY = cy;
                        if (cy > blob.maxY) blob.maxY = cy;

                        // Perimeter: count the 4 sides of this pixel that face
                        // the image border or a non-black pixel (= blob boundary).
                        if (cy == 0        || imageArray[cy-1][cx] != black) blob.perimeter++;
                        if (cy == height-1 || imageArray[cy+1][cx] != black) blob.perimeter++;
                        if (cx == 0        || imageArray[cy][cx-1] != black) blob.perimeter++;
                        if (cx == width-1  || imageArray[cy][cx+1] != black) blob.perimeter++;

                        // Check 4 neighbors for traversal
                        if (cy > 0 && imageArray[cy-1][cx] == black && !visited[cy-1][cx]) { visited[cy-1][cx] = true; stack.push(new int[]{cy-1, cx}); }
                        if (cy < height-1 && imageArray[cy+1][cx] == black && !visited[cy+1][cx]) { visited[cy+1][cx] = true; stack.push(new int[]{cy+1, cx}); }
                        if (cx > 0 && imageArray[cy][cx-1] == black && !visited[cy][cx-1]) { visited[cy][cx-1] = true; stack.push(new int[]{cy, cx-1}); }
                        if (cx < width-1 && imageArray[cy][cx+1] == black && !visited[cy][cx+1]) { visited[cy][cx+1] = true; stack.push(new int[]{cy, cx+1}); }
                    }

                    // Filter out the tiny salt-and-pepper noise!
                    if (blob.pixelCount > 50) {
                        blobs.add(blob);
                    }
                }
            }
        }

        if (blobs.size() < 2) {
            System.out.println("WARNING: Could not distinctively find object and coin. Check mask.");
            PPI = 100; // Safe fallback so it doesn't crash
            return;
        }

        // Object = LARGEST blob (reliable: it dwarfs everything else).
        blobs.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
        Blob mainObject = blobs.get(0);

        // Coin = MOST CIRCULAR blob among the real (non-speck) ones, excluding the
        // object. Identifying the coin by ROUNDNESS instead of size is robust to
        // noise specks (which broke the old "smallest blob" approach: a 60px speck
        // became the coin -> tiny PPI -> exploded dims -> OOM) and to the object
        // fragmenting into multiple blobs (a fragment is elongated, low circularity).
        double minCoinArea = Math.max(200.0, totalPixels / 2000.0); // relative speck floor
        Blob coin = null;
        double bestScore = Double.MAX_VALUE;
        for (Blob bl : blobs) {
            if (bl == mainObject) continue;
            if (bl.pixelCount < minCoinArea) continue;     // skip specks
            double score = Math.abs(bl.circularity() - 1.0);
            if (score < bestScore) { bestScore = score; coin = bl; }
        }

        if (coin == null) {
            // No round, sufficiently-large blob besides the object. Most likely the
            // coin is out of frame for this view (e.g. a top-down shot). Calibration
            // from a coin is impossible here; flag it rather than emit garbage PPI.
            System.out.println("WARNING: No coin-like blob found (coin out of frame?). PPI unreliable.");
            PPI = 100;
            return;
        }

        // PPI from the coin's AREA (solid filled disc): diameter = 2*sqrt(area/pi).
        // Robust to a stray pixel stretching the bbox.
        double coinDiameterPx = 2.0 * Math.sqrt(coin.pixelCount / Math.PI);
        PPI = coinDiameterPx / refSize;

        // Coin exclusion cutoff for DepthPuller (only fires when coin is right of object).
        if (coin.minX > mainObject.maxX) {
            mainEdge = mainObject.maxX + (coin.minX - mainObject.maxX) / 2;
        } else {
            mainEdge = width;
        }

        // Expose both blob boxes so DepthPuller can later exclude the coin
        // position-independently.
        coinMinX = coin.minX; coinMaxX = coin.maxX; coinMinY = coin.minY; coinMaxY = coin.maxY;
        objMinX = mainObject.minX; objMaxX = mainObject.maxX; objMinY = mainObject.minY; objMaxY = mainObject.maxY;

        // Object dimensions (desk-plane PPI; DepthPuller recomputes depth-corrected dims).
        objectWidth = (mainObject.maxX - mainObject.minX) / PPI;
        objectHeight = (mainObject.maxY - mainObject.minY) / PPI;
    }

    // Coin + object bounding boxes from the most recent pixelSize() run.
    public int coinMinX = -1, coinMaxX = -1, coinMinY = -1, coinMaxY = -1;
    public int objMinX = -1, objMaxX = -1, objMinY = -1, objMaxY = -1;

    public double[] mainDimensions(int[][] imageArray) {
        return new double[]{objectWidth, objectHeight};
    }

    public double[] findDimensions(BufferedImage inpt) {
        int[][] arred = toArray(inpt);
        pixelSize(arred);
        return mainDimensions(arred);
    }
}