/*
 * (c) 2024 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.TaskData;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class TaskReportTest {

  private File currentUserHomeDir;
  private File reportDir;
  
  public TaskReportTest() {
  }
  
  @Before
  public void setUp() {
    currentUserHomeDir = new File(java.lang.System.getProperty("java.io.tmpdir"));
    reportDir = new File(currentUserHomeDir, "report-test");
    reportDir.mkdir();
  }
  
  @After
  public void tearDown() {
    var reportFile = new File(reportDir, "tasks.txt");
    reportFile.delete();
    reportDir.delete();
  }

  /**
   * Test of log method, of class TaskReport.
   * @throws java.io.IOException
   */
  @Test
  public void testLog() throws IOException {
    java.lang.System.out.println("log");
    TaskData task = new TaskData();
    task.lId = 123456789;
    task.name = "Test task";
    task.description = "Some task data\nSecond line.";
    
    TaskReport instance = new TaskReport(reportDir.getPath());
    instance.log(task);
    
    var reportFile = new File(reportDir, "tasks.txt");
    var result = Files.readString(reportFile.toPath());
    assertTrue("Task id not found", result.contains("123456789"));
    
    task.lId = 987654321;
    instance.log(task);
    var result2 = Files.readString(reportFile.toPath());
    assertTrue("First task id lost", result2.contains("123456789"));
    assertTrue("Task id not found", result2.contains("987654321"));
    
    reportFile.delete();
  }
  
}
