/*
 * (c) 2024 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

  private static final String F1_TEXT = "File1 Text";
  private static final String F2_TEXT = "File2 Text";
  
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
    
    Files.writeString(file1.toPath(), F1_TEXT, StandardOpenOption.CREATE);
    Files.writeString(file2.toPath(), F2_TEXT, StandardOpenOption.CREATE);
  }
  
  @After
  public void tearDown() {
    file1.delete();
    file2.delete();
    zipDir.delete();
  }

  /**
   * Test of zipDirectory and unzipIntoDirectory methods.
   * @throws java.lang.Exception
   */
  @Test
  public void testZipUnzipDirectory() throws Exception {
    System.out.println("zipUnzipDirectory");
    File sourceDir = zipDir;
    String fileName = "ZIP12345";
    File result = FileUtils.zipDirectory(sourceDir, fileName);
    
    File unzipDir = FileUtils.unzipIntoDirectory(result);
    result.delete();
    
    assertTrue("Unzip directory not found", unzipDir.exists());
    File f1 = new File(unzipDir, "file1.txt");
    String f1Text = Files.readString(f1.toPath());
    f1.delete();
    File f2 = new File(unzipDir, "file2.txt");
    String f2Text = Files.readString(f2.toPath());
    f2.delete();
    unzipDir.delete();
   
    assertEquals("Error reading file 1", f1Text, F1_TEXT);
    assertEquals("Error reading file 2", f2Text, F2_TEXT);
  }

  /**
   * Test of removeExtension method.
   */
  @Test
  public void testRemoveExtension() {
    System.out.println("removeExtension");
    
    String result = FileUtils.removeExtension("test.ext");
    assertEquals("test", result);
    
    result = FileUtils.removeExtension("more.test.ext");
    assertEquals("more.test", result);
    
    result = FileUtils.removeExtension("test.");
    assertEquals("test", result);
    
    result = FileUtils.removeExtension("test");
    assertEquals("test", result);
    
    result = FileUtils.removeExtension(".test");
    assertEquals(".test", result);
    
    result = FileUtils.removeExtension(null);
    assertNull(result);
  }
  
  /**
   * Test of getExtension method.
   */
  @Test
  public void testGetExtension() {
    System.out.println("getExtension");
    
    String result = FileUtils.getExtension("text.ext");
    assertEquals("ext", result);
    
    result = FileUtils.getExtension("text.");
    assertEquals("", result);
    
    result = FileUtils.getExtension("text");
    assertEquals("", result);
  }
  
  /**
   * Test of hasExtension method.
   */
  @Test
  public void testHasExtension() {
    System.out.println("hasExtension");
    
    assertTrue(FileUtils.hasExtension("text.ext", "ext"));
    assertTrue(FileUtils.hasExtension("text.ext", "ExT"));
    assertTrue(FileUtils.hasExtension("text.eXT", "ext"));
    assertFalse(FileUtils.hasExtension("text.ext", "txt"));
    assertFalse(FileUtils.hasExtension("text.", "txt"));
    assertTrue(FileUtils.hasExtension("text.", ""));
  }
}
