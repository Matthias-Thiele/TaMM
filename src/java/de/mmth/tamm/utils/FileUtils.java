/*
 * (c) 2024 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author matthias
 */
public class FileUtils {
  
  /**
   * Zip all files of the given directory in a file.
   * 
   * @param sourceDir
   * @param fileName .zip will be added automatically
   * @return
   * @throws FileNotFoundException
   * @throws IOException 
   */
  public static File zipDirectory(File sourceDir, String fileName) throws FileNotFoundException, IOException {
    File zipFile = new File(sourceDir.getParent(), fileName + ".zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zipOut = new ZipOutputStream(fos)) {
      File[] srcFiles = sourceDir.listFiles();
      for (File srcFile : srcFiles) {
        try (FileInputStream fis = new FileInputStream(srcFile)) {
          ZipEntry zipEntry = new ZipEntry(srcFile.getName());
          zipOut.putNextEntry(zipEntry);
          
          byte[] bytes = new byte[1024];
          int length;
          while((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        }
      }
    }
    
    return zipFile;
  }
}
