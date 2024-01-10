/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.logging.log4j.LogManager;

/**
 * This class is used to obfuscate locally stored password.
 * 
 * It uses AES encryption, but since the encryption password
 * is stored localy and created by the programm, it is not
 * really encrypted. It will just scare off a casual attacker.
 * 
 * @author matthias
 */
public class Obfuscator {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(Obfuscator.class);
  private static final int KEY_LENGTH = 256;
  private static final int ITERATION_COUNT = 32;
  
  private static final byte[] iv = {(byte)0x53, (byte)0xaa, (byte)0x97, (byte)0x31, (byte)0xfa, (byte)0x1d, (byte)0x25, (byte)0x77,
                                    (byte)0x99, (byte)0xe0, (byte)0xd6, (byte)0x01, (byte)0xcc, (byte)0x55, (byte)0x6f, (byte)0x88};
  
  private SecretKey getAesKeyFromPassword(String password, String salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), ITERATION_COUNT, KEY_LENGTH);
    SecretKey key = factory.generateSecret(spec);
    return key;
  }
  
  /**
   * Encrypt the given text with password and salt.
   * Returns a Base64 encoded String.
   * 
   * @param password
   * @param salt
   * @param text
   * @return 
   */
  public String encrypt(String password, String salt, String text) {
    try {
      logger.info("Encrypt " + text);
      SecretKey key = getAesKeyFromPassword(password, salt);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");
      
      logger.info("Prepare Iv.");
      var ivspec = new IvParameterSpec(iv);
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);
      
      logger.info("doFinal");
      byte[] cipherText = cipher.doFinal(text.getBytes("UTF-8"));
      byte[] encryptedData = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, encryptedData, 0, iv.length);
      System.arraycopy(cipherText, 0, encryptedData, iv.length, cipherText.length);
      return Base64.getEncoder().encodeToString(encryptedData);
    } catch (Throwable ex) {
      logger.warn("Obfuscation error.", ex);
      return null;
    }
  }
  
  /**
   * Decrypt the given Base64 Text with password and salt.
   * 
   * @param password
   * @param salt
   * @param text
   * @return 
   */
  public String decrypt(String password, String salt, String text) {
    if (text == null || text.isBlank()) {
      logger.warn("Nothing to decrypt.");
      return "";
    }
    
    try {
      byte[] encryptedData = Base64.getDecoder().decode(text);
      IvParameterSpec ivspec = new IvParameterSpec(iv);
      
      SecretKey key = getAesKeyFromPassword(password, salt);
      SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");
      
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
      
      byte[] cipherText = new byte[encryptedData.length - 16];
      System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);
      
      byte[] decryptedText = cipher.doFinal(cipherText);
      return new String(decryptedText, "UTF-8");
    } catch (Exception ex) {
      logger.warn("Obfuscation error.", ex);
      return "";
    }
    
  }
  
}
