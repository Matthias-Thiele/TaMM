/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import com.google.gson.Gson;
import de.mmth.tamm.ApplicationData;
import de.mmth.tamm.data.AttachmentData;
import de.mmth.tamm.data.JsonResult;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.SessionData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;
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
   * Check if the current session has an assigned client.
   * On first access get the client form the host name
   * and the client configuration.
   * 
   * @param request
   * @param session
   * @param application
   * @return 
   */
  public static boolean checkClientId(HttpServletRequest request, SessionData session, ApplicationData application) {
    String hostName = request.getHeader("host");
    boolean result;
    
    var client = application.getClient(hostName);
    logger.debug("Servlet request from " + session.clientName + " as " + hostName + ((client == null) ? " not found" : client.name));

    if ((session.client == null) && (client != null)) {
      // init client on first access
      session.client = client;
      session.clientName = hostName;
      logger.debug("Initialize new session to client " + client.name);
    }

    result = (client == null) ? false : ((session.client != null) ? session.client.id == client.id : true);

    if (!result) {
      logger.warn("Invalid client access " + ((client == null) ? "null" : client.name) + " of " + session.clientName);
    }
    
    if ((session.user != null) && session.user.mainAdmin) {
      session.clientList = new ArrayList<>(application.clientList.size()); 
      for (var cl: application.clientList) {
        session.clientList.add(new KeyValue(cl.id, cl.name));
      }
    }
    
    return result;
  }
  
  /**
   * Create and populate a JSON result object and send it to the OutputStream.
   * 
   * @param resultData
   * @param isOk
   * @param nextPageOk
   * @param nextPageError
   * @param errorMsg
   * @param data
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
      
      sd.lang = Txt.getMainLanguage();
      var locales = request.getLocales();
      while (locales.hasMoreElements()) {
        Locale locale = locales.nextElement();
        if (Txt.hasLanguage(locale.getLanguage())) {
          sd.lang = locale.getLanguage().substring(0, 2);
          break;
        }
      }
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
  
  /**
   * Calculates and prepares the directory path for an attachment file.
   * 
   * @param fileRoot
   * @param data
   * @return
   * @throws IOException 
   */
  public static File prepareDestinationPath(File fileRoot, AttachmentData data) throws IOException {
    File clientDir = new File(fileRoot, "Client_" + data.clientId);
    File scatterDir = new File(clientDir, "Guid_" + data.guid.substring(0, 2));
    Files.createDirectories(scatterDir.toPath());
    
    File destinationFile = new File(scatterDir, data.guid);
    return destinationFile;
  }
}
