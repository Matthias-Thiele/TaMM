/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author matthias
 */
public class ObfuscatorTest {
  
  public ObfuscatorTest() {
  }

  /**
   * Test of encrypt/ decrypt methods, of class Obfuscator.
   * @throws java.net.UnknownHostException
   */
  @Test
  public void testEncrypt() throws UnknownHostException {
    System.out.println("encrypt");
    String password = "textpassword";
    String salt = InetAddress.getLocalHost().getHostName();
    String text = "my secret #~@\r\u1234";
    
    Obfuscator instance = new Obfuscator();
    String encrypted = instance.encrypt(password, salt, text);
    assertNotEquals("Do not just give the plaintext back.", text, encrypted);
    
    String decrypted = instance.decrypt(password, salt, encrypted);
    assertEquals("Encrypt - Decrypt should not change the text", text, decrypted);
  }


}
