/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import com.google.gson.Gson;
import de.mmth.tamm.data.JsonResult;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author matthias
 */
public class ServletUtils {
  private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ServletUtils.class);
  
  /**
   * Extract the client IP address.
   * 
   * @param request
   * @return 
   */
  public static String getClientIp(HttpServletRequest request) {
    String remoteAddr = "";

    if (request != null) {
      remoteAddr = request.getHeader("X-FORWARDED-FOR");
      if (remoteAddr == null || "".equals(remoteAddr)) {
          remoteAddr = request.getRemoteAddr();
      }
    }

    return remoteAddr;
  }

  /**
   * Convert byte array into hex string.
   * 
   * @param data
   * @return 
   */
  public static String bytesToHex(byte[] data) {
    StringBuilder hexString = new StringBuilder(2 * data.length);
    for (int i = 0; i < data.length; i++) {
      String hex = Integer.toHexString(0xff & data[i]);
      if(hex.length() == 1) {
        hexString.append('0');
      }

      hexString.append(hex);
    }

    return hexString.toString();
  }

  /**
   * Hash password with salt and return it as hex string.
   * 
   * @param password
   * @param salt
   * @return 
   */
  public static String hashPassword(String salt, String password) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      logger.warn("Installed Java does not support SHA-256");
      return "no password available.";
    }

    String saltedPassword = salt + "Z" + password;
    byte[] encodedHash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));  

    return bytesToHex(encodedHash);
  }

  /**
   * Check given password against stored password.
   * 
   * Extract the salt value from the stored password.
   * 
   * @param storedPassword
   * @param queryPassword
   * @return 
   */
  public static boolean comparePassword(String storedPassword, String queryPassword) {
    if (storedPassword.isBlank() && queryPassword.isBlank()) {
      return true; // TODO remove later, don't allow empty passwords
    }
    
    String[] parts = storedPassword.split("Z");
    String salt = parts[0];
    String encodedQuery = salt + "Z" + hashPassword(salt, queryPassword);
    
    return encodedQuery.equals(storedPassword);
  }
  
  /**
   * Encode password with random salt value.
   * 
   * @param password
   * @return 
   */
  public static String encodePassword(String password) {
    String salt = Long.toHexString((long)(Math.random() * Long.MAX_VALUE));
    String encoded = salt + "Z" + hashPassword(salt, password);
    return encoded;
  }
  
  /**
   * Create and populate a JSON result object and send it to the OutputStream.
   * 
   * @param resultData
   * @param isOk
   * @param nextPageOk
   * @param nextPageError
   * @param errorMsg
   * @throws IOException 
   */
  public static void sendResult(OutputStream resultData, boolean isOk, String nextPageOk, String nextPageError, String errorMsg) throws IOException {
    JsonResult result = new JsonResult();
    result.message = errorMsg;
    if (isOk) {
      result.result = "ok";
      result.nextPage = nextPageOk;
    } else {
      result.result = "error";
      result.nextPage = nextPageError;
    }
    
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(result, writer);
    }
  }
  
}
