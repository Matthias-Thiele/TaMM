/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.TammError;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class TemplateCacheTest {

  private static File templateDir;
  private static File mail;
  private static File currentUserHomeDir;
  private static File templateSubDir;
  
  public TemplateCacheTest() {
  }
  
  @BeforeClass
  public static void setUpClass() throws IOException {
    currentUserHomeDir = new File(System.getProperty("java.io.tmpdir"));
    templateDir = new File(currentUserHomeDir, "template-test");
    templateDir.mkdir();
    templateSubDir = new File(templateDir, "mail");
    templateSubDir.mkdir();
    mail = new File(templateSubDir, "filemailtemplate");
    Files.writeString(mail.toPath(), "file mailtemplate", StandardOpenOption.CREATE_NEW);
  }
  
  @AfterClass
  public static void tearDownClass() {
    mail.delete();
    templateDir.delete();
  }

  /**
   * Test of setFileRoot method, of class TemplateCache.
   * @throws de.mmth.tamm.TammError
   */
  @Test
  public void testSetFileRoot() throws TammError {
    System.out.println("setFileRoot");
    TemplateCache instance = new TemplateCache("template-test");
    instance.setFileRoot(currentUserHomeDir);
    
    String test = instance.getTemplate("mail/filemailtemplate");
    assertFalse(test.isEmpty());
    
    String test2 = instance.getTemplate("mail/mailtemplate");
    assertFalse(test2.isEmpty());
    
    instance.clear();
  }

}
