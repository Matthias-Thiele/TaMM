/*
 * (c) 2024 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class FileUtilsTest {

  private File currentUserHomeDir;
  private File zipDir;
  private File file1;
  private File file2;
  
  public FileUtilsTest() {
  }
  
  @Before
  public void setUp() throws IOException {
    currentUserHomeDir = new File(System.getProperty("java.io.tmpdir"));
    zipDir = new File(currentUserHomeDir, "zip-test");
    zipDir.mkdir();
    
    file1 = new File(zipDir, "file1.txt");
    file2 = new File(zipDir, "file2.txt");
    
    Files.writeString(file1.toPath(), "File1 Text", StandardOpenOption.CREATE);
    Files.writeString(file2.toPath(), "File2 Text", StandardOpenOption.CREATE);
  }
  
  @After
  public void tearDown() {
    file1.delete();
    file2.delete();
    zipDir.delete();
  }

  /**
   * Test of zipDirectory method, of class FileUtils.
   */
  @Test
  public void testZipDirectory() throws Exception {
    System.out.println("zipDirectory");
    File sourceDir = zipDir;
    String fileName = "ZIP12345";
    File result = FileUtils.zipDirectory(sourceDir, fileName);
    byte[] zip = Files.readAllBytes(result.toPath());
    result.delete();
    assertTrue(zip.length == 266);
  }
  
}
