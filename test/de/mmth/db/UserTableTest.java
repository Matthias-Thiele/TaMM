/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package de.mmth.db;

import de.mmth.TammError;
import de.mmth.data.UserData;
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
    con.dropDB("test");
    con.close();
  }

  /**
   * Test of readUser and writeUser methods, of class UserTable.
   */
  @Test
  public void testWriteReadUser() throws TammError {
    System.out.println("writeReadUser");
    UserTable instance = new UserTable(con, "testusers");
    
    UserData user = new UserData();
    user.name = "Test1";
    user.pwd = "asdf";
    user.mail = "test1@test.de";
    user.isMainAdmin = true;
    user.supervisorId = 12345;
    instance.writeUser(user);
    
    UserData user2 = instance.readUser(-1, "Test1");
    assertEquals("User name mismatch", user.name, user2.name);
    assertEquals("Password mismatch", user.pwd, user2.pwd);
    assertEquals("EMail mismatch", user.mail, user2.mail);
    assertEquals("Main Admin mismatch", user.isMainAdmin, user2.isMainAdmin);
    assertEquals("Subadmin mismatch", user.isSubAdmin, user2.isSubAdmin);
    assertEquals("Supervisor mismatch", user.supervisorId, user2.supervisorId);
    
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
  }

}
