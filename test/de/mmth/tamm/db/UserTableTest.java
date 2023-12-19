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
   * Test of readUser and writeUser methods, of class UserTable.
   */
  @Test
  public void testWriteReadUser() throws TammError {
    System.out.println("writeReadUser");
    UserTable instance = new UserTable(con, "testusers");
    // admin user with id=1 automatically created
    
    UserData user = new UserData();
    user.name = "Test1";
    user.pwd = "asdf";
    user.mail = "test1@test.de";
    user.isMainAdmin = true;
    user.supervisorId = 12345;
    user.administratorId = 9999;
    
    instance.writeUser(user);
    
    UserData user2 = instance.readUser(-1, "Test1");
    assertEquals("User name mismatch", user.name, user2.name);
    assertEquals("Password mismatch", user.pwd, user2.pwd);
    assertEquals("EMail mismatch", user.mail, user2.mail);
    assertEquals("Main Admin mismatch", user.isMainAdmin, user2.isMainAdmin);
    assertEquals("Subadmin mismatch", user.isSubAdmin, user2.isSubAdmin);
    assertEquals("Supervisor mismatch", user.supervisorId, user2.supervisorId);
    assertEquals("Administrator mismatch", user.administratorId, user2.administratorId);
    
    UserData user3 = instance.readUser(user2.id, null);
    assertEquals("User name mismatch", user3.name, user2.name);
    
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
    instance.writeUser(user5);
    
    List<UserData> users1 = instance.listUsers(-1);
    assertEquals("Three users expected.", users1.size(), 3);
    
    List<UserData> users2 = instance.listUsers(2);
    assertEquals("One user expected.", users2.size(), 1);
    assertEquals("User Test2 expected.", users2.get(0).name, user5.name);
    
  }

}
