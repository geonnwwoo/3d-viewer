package lhs.finalproject;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Unzipper {

    //unzips the files that iphone shortcut has
    public static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());

                newFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }
}


