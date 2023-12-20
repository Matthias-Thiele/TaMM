/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class PasswordUtilsTest {
  
  /**
   * Test of bytesToHex method, of class ServletUtils.
   */
  @Test
  public void testBytesToHex() {
    System.out.println("bytesToHex");
    byte[] data = {(byte)0x12, (byte)0xef};
    String expResult = "12ef";
    String result = PasswordUtils.bytesToHex(data);
    assertEquals(expResult, result);
  }

  /**
   * Test of hashPassword method, of class ServletUtils.
   */
  @Test
  public void testHashPassword() {
    System.out.println("hashPassword");
    String password = "some password";
    String salt = "1234567890";
    String expResult = "0c2d5de14f29c244480bade9436b582888e2b9a69c2293c6ebbad901754de802";
    String result = PasswordUtils.hashPassword(salt, password);
    assertEquals(expResult, result);
  }

  /**
   * Test of comparePassword method, of class ServletUtils.
   */
  @Test
  public void testComparePassword() {
    System.out.println("comparePassword");
    String storedPassword = "1234567890Z0c2d5de14f29c244480bade9436b582888e2b9a69c2293c6ebbad901754de802";
    String queryPassword = "some password";
    boolean expResult = true;
    boolean result = PasswordUtils.comparePassword(storedPassword, queryPassword);
    assertEquals(expResult, result);
  }

}
