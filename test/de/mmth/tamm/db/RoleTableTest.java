/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.db;

import de.mmth.tamm.data.RoleData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class RoleTableTest {
  
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
    String[] cols = RoleTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 4, cols.length);
  }
  
  /**
   * Test of writeClient method, of class RoleTable.
   * @throws java.lang.Exception
   */
  @Test
  public void testWriteClient() throws Exception {
    System.out.println("writeClient");
    RoleTable instance = new RoleTable(con, "testroles");
    
    RoleData role = new RoleData();
    role.id = -1;
    role.clientId = 1;
    role.name = "Test role 1";
    role.owner = 99;
    
    int result = instance.writeRole(role);
    assertEquals("Role ids start with 0x1000000", 16777216, result);
    
    int clientId = instance.getRoleClient(result);
    assertEquals("ClientId mismatch", role.clientId, clientId);
    
    var roles = instance.listRoles(role.clientId, role.owner);
    assertEquals("Only one role written", 1, roles.size());
    
    RoleData role2 = new RoleData();
    role2.id = -1;
    role2.clientId = 2;
    role2.name = "Test role 2";
    role2.owner = 10;
    
    int result2 = instance.writeRole(role2);
    assertEquals("Role ids start with 0x1000000", 16777217, result2);
    
    roles = instance.listRoles(role.clientId, role.owner);
    assertEquals("Still only one role written for clientId = 1", 1, roles.size());
    
    RoleData role3 = new RoleData();
    role3.id = -1;
    role3.clientId = 1;
    role3.name = "Test role 3";
    role3.owner = 98;
    int result3 = instance.writeRole(role3);
    assertEquals("Role ids start with 0x1000000", 16777218, result3);
    
    roles = instance.listRoles(role.clientId, role.owner);
    assertEquals("Still only one role written for clientId = 1 and owner 99", 1, roles.size());
    
    roles = instance.listRoles(role.clientId, role3.owner);
    assertEquals("Still only one role written for clientId = 1 and owner 98", 1, roles.size());
    
    roles = instance.listRoles(role.clientId, -1);
    assertEquals("Two roles written for clientId = 1 and owner 99 and 98", 2, roles.size());
    
    roles = instance.listRoles(role2.clientId, role2.owner);
    assertEquals("Only one role written for clientId = 2", 1, roles.size());
    
    instance.removeRole(role2.clientId, result2);
    roles = instance.listRoles(role2.clientId, role2.owner);
    assertEquals("The one role written for clientId = 2 has been deleted", 0, roles.size());
  }
  
}
