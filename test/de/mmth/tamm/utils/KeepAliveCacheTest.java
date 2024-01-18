/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.io.File;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author matthias
 */
public class KeepAliveCacheTest {

  private static Path savePath;
  
  @BeforeClass
  public static void setUpClass() {
    String currentUsersHomeDir = System.getProperty("java.io.tmpdir");
    savePath = new File(currentUsersHomeDir, "test-keep-alive.txt").toPath();
  }
  
  @AfterClass
  public static void tearDownClass() {
    savePath.toFile().delete();
  }

  /**
   * Test of addLogin method, of class KeepAliveCache.
   * @throws java.lang.InterruptedException
   */
  @Test
  public void testKeepAliveLogin() throws InterruptedException {
    System.out.println("addLogin");
    int userId = 23;
    long keepAliveDuration = 0L;
    KeepAliveCache instance = new KeepAliveCache();
    
    String result = instance.addLogin(userId, keepAliveDuration);
    
    int returnId = instance.getUserId(result);
    assertEquals("cookie has been created before", returnId, userId);
    
    returnId = instance.getUserId("x" + result);
    assertEquals("Invalid/ unknown cookie, no user should be found.", -1, returnId);
    
    Thread.sleep(10);
    instance.cleanup();
    
    returnId = instance.getUserId(result);
    assertEquals("Cache has been cleared, no user should be found.", -1, returnId);
    
    String result2 = instance.addLogin(userId, keepAliveDuration);
    int returnId2 = instance.getUserId(result2);
    assertEquals("New user has been created", userId, returnId2);
    
    instance.remove(result2);
    int returnId3 = instance.getUserId(result2);
    assertEquals("User logout has been done, no user should be found.", -1, returnId3);
  }

  /**
   * Test of cleanup method, of class KeepAliveCache.
   */
  @Test
  public void testKeepAliveLoginWithDuration() {
    System.out.println("addLogin with duration");
    int userId = 23;
    long keepAliveDuration = 10000L;
    KeepAliveCache instance = new KeepAliveCache();
    
    String result = instance.addLogin(userId, keepAliveDuration);
    
    int returnId = instance.getUserId(result);
    assertEquals("cookie has been created before", returnId, userId);
    
    instance.cleanup();
    
    returnId = instance.getUserId(result);
    assertEquals("KeepAlive duration not reached, user should be found.", userId, returnId);
  }

  /**
   * Test of persisting data, of class KeepAliveCache.
   */
  @Test
  public void testKeepAliveLoginPersist() {
    System.out.println("persist test");
    int userId = 23;
    long keepAliveDuration = 10000L;
    KeepAliveCache instance = new KeepAliveCache();
    
    String result = instance.addLogin(userId, keepAliveDuration);
    
    int returnId = instance.getUserId(result);
    assertEquals("cookie has been created before", returnId, userId);
    
    instance.save(savePath);
    
    var instance2 = new KeepAliveCache();
    instance2.load(savePath);
    
    returnId = instance.getUserId(result);
    assertEquals("User should be found.", userId, returnId);
  }

  /**
   * Test of persisting data, of class KeepAliveCache.
   */
  @Test
  public void testKeepAliveMax3() throws InterruptedException {
    System.out.println("max 3 test");
    int userId = 23;
    long keepAliveDuration = 10000L;
    KeepAliveCache instance = new KeepAliveCache();
    
    String result1 = instance.addLogin(userId, keepAliveDuration);
    Thread.sleep(5);
    String result2 = instance.addLogin(userId, keepAliveDuration);
    Thread.sleep(2);
    String result3 = instance.addLogin(userId, keepAliveDuration);
    Thread.sleep(2);
    String result4 = instance.addLogin(userId, keepAliveDuration);
    
    int returnId = instance.getUserId(result2);
    assertEquals("should be still alive", returnId, userId);
    returnId = instance.getUserId(result3);
    assertEquals("should be still alive", returnId, userId);
    returnId = instance.getUserId(result4);
    assertEquals("should be still alive", returnId, userId);
    
    returnId = instance.getUserId(result1);
    assertEquals("should been evicted", -1, returnId);
  }

}
