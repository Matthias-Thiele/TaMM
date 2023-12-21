/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import com.google.gson.Gson;
import de.mmth.tamm.data.JsonResult;
import de.mmth.tamm.data.SessionData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
   * Create and populate a JSON result object and send it to the OutputStream.
   * 
   * @param resultData
   * @param isOk
   * @param nextPageOk
   * @param nextPageError
   * @param errorMsg
   * @throws IOException 
   */
  public static void sendResult(OutputStream resultData, boolean isOk, String nextPageOk, String nextPageError, String errorMsg, Object data) throws IOException {
    JsonResult result = new JsonResult();
    result.message = errorMsg;
    result.data = data;
    
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
  
  /**
   * Returns the session data object.
   * 
   * When the session is new, a new session data
   * object will be created and populated with
   * initial data.
   * 
   * @param request
   * @return 
   */
  public static SessionData prepareSession(HttpServletRequest request) {
    HttpSession session = request.getSession();
    SessionData sd = (SessionData) session.getAttribute("TAMM");
    if (sd == null) {
      sd = new SessionData();
      session.setAttribute("TAMM", sd);
      sd.clientIp = ServletUtils.getClientIp(request);
      logger.info("New session from address " + sd.clientIp);
    }
    
    return sd;
  }

  /**
   * Send result with error marker and link to error page.
   * 
   * @param resultData
   * @throws IOException 
   */
  public static void gotoErrorPage(OutputStream resultData) throws IOException {
    JsonResult result = new JsonResult();
    result.result = "ok";
    result.nextPage = "error.html";
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(result, writer);
    }
  }
  
  
}
