/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm.db;

import de.mmth.tamm.TammError;
import de.mmth.tamm.data.UserData;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class UserTableTest {

  private static DBConnect con;
  
  public UserTableTest() {
  }
  
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
    String[] cols = UserTable.TABLE_CONFIG.split("\\R");
    assertEquals("Number of columns changed", 8, cols.length);
  }
  
  /**
   * Test of readUser and writeUser methods, of class UserTable.
   */
  @Test
  public void testWriteReadUser() throws TammError {
    System.out.println("writeReadUser");
    UserTable instance = new UserTable(con, "testusers");
    // admin user with id=1 automatically created
    
    UserData user = new UserData();
    user.name = "Test1";
    user.pwd = "";
    user.mail = "mtest1@test.de";
    user.mainAdmin = true;
    user.supervisorId = 12345;
    user.administratorId = 9999;
    user.lastLogin = "20231231112233";
    
    instance.writeUser(user);
    
    UserData user2 = instance.readUser(-1, "Test1");
    assertEquals("User name mismatch", user.name, user2.name);
    assertEquals("EMail mismatch", user.mail, user2.mail);
    assertEquals("Main Admin mismatch", user.mainAdmin, user2.mainAdmin);
    assertEquals("Subadmin mismatch", user.subAdmin, user2.subAdmin);
    assertEquals("Supervisor mismatch", user.supervisorId, user2.supervisorId);
    assertEquals("Administrator mismatch", user.administratorId, user2.administratorId);
    assertEquals("Last login date mismatch", user.lastLogin, user2.lastLogin);
    
    String newLoginDate = "2024010122334";
    instance.updateLoginDate(user2.id, newLoginDate);
    UserData user3 = instance.readUser(user2.id, null);
    assertEquals("User name mismatch", user3.name, user2.name);
    assertEquals("Last login date mismatch", newLoginDate, user3.lastLogin);
    
    user3.name = "Test1 name changed";
    instance.writeUser(user3);
    
    UserData user4 = instance.readUser(user2.id, null);
    assertEquals("Update error.", user3.name, user4.name);
    
    try {
      instance.readUser(-1, "Unknown user");
      fail("Reading an unknown user should have raised an exception.");
    } catch(TammError e) {
      // as expected
    }
    
    UserData user5 = new UserData();
    user5.name = "Test2";
    user5.administratorId = 2;
    user5.mail = "mtest2@test.de";
    instance.writeUser(user5);
    
    List<UserData> users1 = instance.listUsers(-1, null);
    assertEquals("Three users expected.", users1.size(), 3);
    
    List<UserData> users2 = instance.listUsers(2, null);
    assertEquals("One user expected.", users2.size(), 1);
    assertEquals("User Test2 expected.", users2.get(0).name, user5.name);

    List<UserData> users3 = instance.listUsers(-1, "test%");
    assertEquals("Two users expected.", users3.size(), 2);

    List<UserData> users4 = instance.listUsers(2, "MTest%");
    assertEquals("Only user test2 expected.", users4.size(), 1);
  }

}
