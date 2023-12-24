/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.TaskData;
import de.mmth.tamm.utils.DateUtils;
import java.time.ZonedDateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class TaskTableTest {
  
  private static DBConnect con;
  
  @BeforeClass
  public static void setUpClass() {
    con = new DBConnect("jdbc:postgresql://localhost:5432/postgres", "test", "postgres", "postgres");
  }
  
  @AfterClass
  public static void tearDownClass() {
    con.dropDB();
    con.close();
  }
  
  /**
   * Test if the number of columns has been changed
   * without changing the unit tests.
   */
  @Test
  public void testCheckColumns() {
    String[] cols = TaskTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 10, cols.length);
  }

  /**
   * Test of writeTask method, of class TaskTable.
   */
  @Test
  public void testWriteReadTask() throws Exception {
    System.out.println("writeTask");
    TaskData task = new TaskData();
    task.lId = -1;
    task.name = "First test task";
    task.description = "This is the description of the new task.";
    task.createDate = DateUtils.formatZ(null);
    task.lastChanged = DateUtils.formatZ(DateUtils.now().plusMinutes(2));
    task.creator = 123;
    ZonedDateTime dueDate = DateUtils.now().plusDays(1);
    task.nextDueDate = DateUtils.formatZ(dueDate);
    task.startDate = DateUtils.formatZ(DateUtils.now().plusDays(3));
    task.owner = 456;
    task.interval = "single|1|2024-01-02";
    
    TaskTable instance = new TaskTable(con, "testtasks");
    long result = instance.writeTask(task);
    assertTrue("Task long id malformed", result > 0);
    
    System.out.println("readTask");
    TaskData task2 = instance.readTask(result);
    assertEquals("Task Id mismatch", result, task2.lId);
    assertEquals("Task name mismatch", task.name, task2.name);
    assertEquals("Task description mismatch", task.description, task2.description);
    assertEquals("Task creator mismatch", task.creator, task2.creator);
    assertEquals("Task create date mismatch", task.createDate, task2.createDate);
    assertEquals("Task last changed date mismatch", task.lastChanged, task2.lastChanged);
    assertEquals("Task due date mismatch", task.nextDueDate, task2.nextDueDate);
    assertEquals("Task start date mismatch", task.startDate, task2.startDate);
    assertEquals("Task owner mismatch", task.owner, task2.owner);
    assertEquals("Task interval mismatch", task.interval, task2.interval);
    
    try {
      instance.readTask(12345);
      fail("Reading an unknown task should have raised an exception.");
    } catch(TammError e) {
      // as expected
    }
    
    // Test update
    task2.name = "changed to test2";
    instance.writeTask(task2);
    
    TaskData task3 = instance.readTask(task2.lId);
    assertEquals("Task name mismatch", task2.name, task3.name);
  }

}
