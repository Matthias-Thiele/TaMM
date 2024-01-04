/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.data.RoleAssignmentData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class RoleAssignmentTableTest {
  
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
    String[] cols = RoleAssignmentTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 2, cols.length);
  }
  
  /**
   * Test of writeRoleAssignments method, of class RoleAssignmentTable.
   * @throws java.lang.Exception
   */
  @Test
  public void testReadWriteRoleAssignments() throws Exception {
    System.out.println("writeReadRoleAssignments");
    RoleAssignmentTable instance = new RoleAssignmentTable(con, "testroleassignments");
    
    RoleAssignmentData roles = new RoleAssignmentData();
    roles.user = 12345;
    roles.roles = new int[] {100, 101, 102};
    instance.writeRoleAssignments(roles);
    
    var reread = instance.listRoleAssignments(roles.user);
    assertEquals("Read back should give the same roles list.", roles.roles.length, reread.roles.length);
    
    instance.writeRoleAssignments(roles); // again
    
    var reread2 = instance.listRoleAssignments(roles.user);
    assertEquals("Read back still should give the same roles list.", roles.roles.length, reread2.roles.length);
    
    RoleAssignmentData roles2 = new RoleAssignmentData();
    roles2.user = 12346;
    roles2.roles = new int[] {99, 101, 102};
    instance.writeRoleAssignments(roles2);
    
    instance.clearRoleAssignments(101, false); 
    var reread3a = instance.listRoleAssignments(roles.user);
    assertEquals("Role 101 has been deleted.", roles.roles.length - 1, reread3a.roles.length);
    var reread3b = instance.listRoleAssignments(roles2.user);
    assertEquals("Role 101 has been deleted.", roles2.roles.length - 1, reread3b.roles.length);
    
    instance.clearRoleAssignments(roles.user, true);
    
    var reread4 = instance.listRoleAssignments(roles.user);
    assertEquals("List has been cleared, should be empty.", 0, reread4.roles.length);
  }

}
