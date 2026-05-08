package lhs.finalproject;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EXIF_info {
    static File image = new File("src/main/resources/test_images/exif_with_reference.jpg");
    public BufferedImage convertBW(File image){
//        try {
//            Metadata imageData = ImageMetadataReader.readMetadata(image);
//            System.out.println("pulled all data!");
//            for (Directory dir: imageData.getDirectories()){
//                for (Tag tag: dir.getTags()){
//                    System.out.println(tag);
//                }
//            }
//        } catch (IOException | ImageProcessingException e) {
//            throw new RuntimeException(e);
//        }

        try{
            BufferedImage ogImage = ImageIO.read(image);
            BufferedImage bwImage = new BufferedImage(ogImage.getWidth(), ogImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

            Graphics2D g2d = bwImage.createGraphics();
            g2d.drawImage(ogImage,0 , 0, null);
            g2d.dispose();

            return bwImage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args){
        EXIF_info processor = new EXIF_info();

        BufferedImage bw = processor.convertBW(image);

        try {
            File outputFile = new File("convertedImage.png");
            ImageIO.write(bw, "png", outputFile);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
