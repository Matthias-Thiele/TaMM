/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import de.mmth.tamm.data.UserData;
import java.io.File;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class RequestCacheTest {

  private static Path savePath;
  
  public RequestCacheTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
    String currentUsersHomeDir = System.getProperty("java.io.tmpdir");
    savePath = new File(currentUsersHomeDir, "test-requests.txt").toPath();
  }
  
  @AfterClass
  public static void tearDownClass() {
    savePath.toFile().delete();
  }

  /**
   * Test of add method, of class RequestCache.
   */
  @Test
  public void testAdd() {
    System.out.println("add");
    String myIp = "192.168.1.1";
    UserData user1 = new UserData();
    user1.id = 123;
    user1.name = "test1";
    user1.mail = "test1@test.de";
    
    UserData user2 = new UserData();
    user2.id = 999;
    user2.name = "test2";
    user2.mail = "test2@test.de";
    
    long duration = 100000L;
    RequestCache instance = new RequestCache();
    String key1 = instance.add(user1, duration, myIp);
    assertFalse(key1.isEmpty());
    
    System.out.println("getUserItem");
    String user3mail = instance.getUserMail(key1);
    assertEquals(user1.mail, user3mail);
    int user3id = instance.getUserId(key1);
    assertEquals(user1.id, user3id);
    
    System.out.println("save");
    String key2 = instance.add(user2, duration, myIp);
    instance.save(savePath);
    
    RequestCache instance2 = new RequestCache();
    instance2.load(savePath);
    
    String user1A = instance2.getUserMail(key1);
    assertNotNull(user1A);
    assertEquals(user1.mail, user1A);
    
    String user2A = instance2.getUserMail(key2);
    assertNotNull(user2A);
    assertEquals(user2.mail, user2A);
  }

  
}
