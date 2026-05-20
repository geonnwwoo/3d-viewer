package lhs.finalproject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class image_dimensions {

    static final double  refSize = 1.0;
    static BufferedImage image;

    public image_dimensions(File inputImage){
        try {
            image = ImageIO.read(inputImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BufferedImage HSVMask(BufferedImage originalImage){
        if(originalImage == null){
            System.out.println("image is null");
            return null;
        }
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        float lowH = 0.0f, highH = 1.0f;
        float lowS = 0.0f, highS = 0.10f;
        float lowB = 0.87f, highB = 1.0f;
        float[] hsb = new float[3];
        for (int h = 0; h < height; h++){
            for (int w = 0; w < width; w++){
                int rgb = originalImage.getRGB(w,h);

                int r = (rgb >>16) & 0xFF;
                int g = (rgb >>8) & 0xFF;
                int b = rgb & 0xFF;

                Color.RGBtoHSB(r,g,b,hsb);

                if (hsb[0] >= lowH && hsb[0] <= highH && hsb[1] >= lowS && hsb[1] <= highS && hsb[2] >= lowB && hsb[2] <= highB){
                    mask.setRGB(w, h, 0xFFFFFFFF);
                }
                else{
                    mask.setRGB(w, h, 0xFF000000);
                }
            }
        }
        return mask;
    }

    public int findSplitY(BufferedImage image){
        //todo
        return 0;
    }

    public int[] getPixelDimensions(BufferedImage image){
        //todo
        return null;
    }

    public double PPI(double width1, double width2){
        //todo
        return 0.0;
    }

    public double[] finalSize(int[] pixelDimensions, double ratio){
        //todo
        return null;
    }

    public static void showImagePopup(BufferedImage img, String windowTitle) {
        JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // 1. Point this to your local jpg
        File testFile = new File("src/main/resources/test_images/{FIXTHIS}.jpg");

        // 2. Initialize your class
        image_dimensions processor = new image_dimensions(testFile);

        // 3. Generate the mask using your class-level image
        System.out.println("Generating mask...");
        BufferedImage maskedResult = processor.HSVMask(image);

        // 4. Show the original and the mask side-by-side in popups!
        System.out.println("Popping up windows. Close the window to end the program.");
        showImagePopup(image, "Original JPG");
        showImagePopup(maskedResult, "HSV Mask Test");
    }

}
