/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.data.AttachmentData;
import java.util.List;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class AttachmentTableTest {
  
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
    var cols = AttachmentTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 4, cols.length);
  }

  /**
   * Test of write, list and delete methods, of class AttachmentTable.
   * @throws java.lang.Exception
   */
  @Test
  public void testWriteReadDeleteAttachment() throws Exception {
    System.out.println("Process attachments.");
    int clientId = 99;
    long taskId = 1234567890;
    
    AttachmentTable instance = new AttachmentTable(con, "testattachments");
    
    AttachmentData att = new AttachmentData();
    att.clientId = clientId;
    att.fileName = "testfile1.something";
    att.guid = UUID.randomUUID().toString();
    att.taskId = taskId;
    instance.writeAttachment(att);
    
    List<AttachmentData> result = instance.listAttachments(clientId, taskId);
    assertEquals("Exactly 1 item stored.", 1, result.size());
    AttachmentData res = result.get(0);
    assertEquals("ClientId mismatch", att.clientId, res.clientId);
    assertEquals("File name mismatch", att.fileName, res.fileName);
    assertEquals("Task id mismatch", att.taskId, res.taskId);
    assertEquals("Guid mismatch", att.guid, res.guid);
    
    AttachmentData att2 = new AttachmentData();
    att2.clientId = clientId;
    att2.fileName = "testfile2.something";
    att2.guid = UUID.randomUUID().toString();
    att2.taskId = taskId;
    instance.writeAttachment(att2);
    
    List<AttachmentData> result4 = instance.listAttachments(clientId, taskId);
    assertEquals("Exactly 2 items stored (att and att2).", 2, result4.size());

    List<AttachmentData> result2 = instance.listAttachments(clientId, taskId + 1);
    assertEquals("Non-existing task, no item expected.", 0, result2.size());
    
    instance.removeAttachment(att2);
    List<AttachmentData> result3 = instance.listAttachments(clientId, taskId);
    assertEquals("Only one of the two existing items should have been deleted.", 1, result3.size());
    
    instance.removeAttachment(att);
    List<AttachmentData> result5 = instance.listAttachments(clientId, taskId);
    assertEquals("The only one item should have been deleted.", 0, result5.size());
  }
  
}
