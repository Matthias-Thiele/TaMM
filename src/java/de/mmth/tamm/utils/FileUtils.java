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
import java.util.zip.ZipInputStream;
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
  
  /**
   * Unzips the given file into a directory with the same name.
   * 
   * The destination directory lies side by side to the source
   * file and has the same name but no extension.
   * 
   * @param sourceFile
   * @return 
   */
  public static File unzipIntoDirectory(File sourceFile) throws FileNotFoundException, IOException {
    var dirName = removeExtension(sourceFile.getName());
    var dir = new File(sourceFile.getParentFile(), dirName);
    if (!dir.exists()) {
      dir.mkdir();
    }
    
    var buffer = new byte[1000];
    try (FileInputStream zipData = new FileInputStream(sourceFile)) {
      ZipInputStream zis = new ZipInputStream(zipData);
      for (;;) {
        var zipEntry = zis.getNextEntry();
        if (zipEntry == null) {
          break;
        }
        
        var fileName = zipEntry.getName();
        var file = new File(dir, fileName);
        try (FileOutputStream unzippedData = new FileOutputStream(file)) {
          for (;;) {
            var len = zis.read(buffer);
            if (len < 1) {
              break;
            }
            
            unzippedData.write(buffer, 0, len);
          }
        }
        
        zis.closeEntry();
      }
    }
    
    return dir;
  }
  
  /**
   * Removes the extension from the given file name.
   * 
   * @param fileNameWithExtension
   * @return 
   */
  public static String removeExtension(String fileNameWithExtension) {
    if (fileNameWithExtension == null) {
      return null;
    }
    
    var dotPos = fileNameWithExtension.lastIndexOf('.');
    if (dotPos < 1) {
      return fileNameWithExtension; // no extension found
    } else {
      return fileNameWithExtension.substring(0, dotPos);
    }
  }
  
  /**
   * Returns the extension of the given file name
   * @param fileNameWithExtension
   * @return 
   */
  public static String getExtension(String fileNameWithExtension) {
    if (fileNameWithExtension == null) {
      return null;
    }
    
    var dotPos = fileNameWithExtension.lastIndexOf('.');
    if (dotPos < 1) {
      return ""; // no extension found
    } else {
      return fileNameWithExtension.substring(dotPos + 1);
    }
    
  }
  
  /**
   * Checks if the given file name has the given extension (case insensitive)
   * @param fileName
   * @param extension
   * @return 
   */
  public static boolean hasExtension(String fileName, String extension) {
    var ext = getExtension(fileName);
    return ext.compareToIgnoreCase(extension) == 0;
  }
}
