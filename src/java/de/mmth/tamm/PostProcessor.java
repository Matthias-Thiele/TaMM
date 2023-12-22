/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.LoginData;
import de.mmth.tamm.data.SessionData;
import com.google.gson.Gson;
import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.JsonResult;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.DateUtils;
import de.mmth.tamm.utils.PasswordUtils;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class PostProcessor {
  private static final Logger logger = LogManager.getLogger(PostProcessor.class);
  private static final long PWD_REQUEST_VALID_MILLIS = 3600000;
  
  private final ApplicationData application;
  private final UserProcessor userProcessor;
  
  /**
   * Constructor with global application data.
   * 
   * @param application 
   */
  public PostProcessor(ApplicationData application) {
    this.application = application;
    this.userProcessor = new UserProcessor(application);
  }
  
  /**
   * Process incoming post request.
   * 
   * @param session
   * @param uri
   * @param sourceData
   * @param resultData
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String uri, InputStream sourceData, OutputStream resultData) throws IOException, TammError {
    String[] uriParts = uri.split("/");
    String cmd = uriParts[3];
    if (!application.checkInit() && !cmd.equals("initdata")) {
      logger.warn("Missing initialisation data, request aborted.");
      ServletUtils.gotoErrorPage(resultData);
      return;
    }
    
    try (Reader reader = new InputStreamReader(sourceData)) {
      switch (uriParts[3]) {
        case "login":
          processLogin(reader, resultData, session);
          break;
          
        case "initdata":
          processInitdata(reader, resultData, session);
          break;
          
        case "filter":
          userProcessor.processFilter(reader, resultData, session);
          break;
          
        case "saveuser":
          userProcessor.processSaveUser(reader, resultData, session);
          break;
          
        case "pwdreq":
          processPasswordRequest(reader, resultData);
          break;
          
        case "updatepwd":
          processUpdatePassword(reader, resultData);
          break;
      }
    }
  }
  
  /**
   * Validates user and password.
   * 
   * @param reader
   * @param resultData
   * @throws IOException 
   */
  private void processLogin(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    LoginData loginData = new Gson().fromJson(reader, LoginData.class);
    logger.debug("Process login request for user" + loginData.name);
    JsonResult result = new JsonResult();
    try {
    var user = application.users.readUser(-1, loginData.name);
    if (PasswordUtils.comparePassword(user.pwd, loginData.pwd)) {
      result.result = "ok";
      result.nextPage = "index.html";
      session.loginTime = DateUtils.format(null); 
      session.user = user;
      session.user.pwd = ""; // do not leak password.
      application.users.updateLoginDate(user.id, session.loginTime);
    } else {
      result.result = "error";
      result.nextPage = "";
      result.message = "Unbekannter Anwender oder falsches Passwort.";
    }
    } catch( TammError ex) {
      result.result = "error";
      result.nextPage = "";
      result.message = "Unbekannter Anwender oder falsches Passwort.";
    }
    
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(result, writer);
    }
  }
  
  /**
   * Stores system administration data.
   * 
   * @param reader
   * @param resultData
   * @throws IOException 
   */
  private void processInitdata(Reader reader, OutputStream resultData, SessionData session) throws IOException {
    AdminData adminData = new Gson().fromJson(reader, AdminData.class);
    boolean isMainAdmin = (session.user != null) && session.user.mainAdmin;
    boolean ok;
    if (application.db.isValid() && !isMainAdmin) {
      logger.warn("Invalid application data write attempt ignored from " + session.clientIp);
      ok = false;
    } else {
      logger.warn("Servlet Initdata written " + adminData.dburl);
      application.setAdminData(adminData);
      ok = application.checkInit();
    }
    
    ServletUtils.sendResult(resultData, ok, "index.html", "admin.html", "", null);
  }
  
  
  /**
   * Processes new password request.
   * 
   * @param reader
   * @param resultData
   * @throws IOException 
   */
  private void processPasswordRequest(Reader reader, OutputStream resultData) throws IOException {
    UserData userData = new Gson().fromJson(reader, UserData.class);
    logger.debug("Password request for user " + userData.name);
    boolean found = false;
    String message = "Unbekannter Anwender oder Mailadresse";
    try {
      var checkUser = application.users.readUser(-1, userData.name);
      if (checkUser.name.equalsIgnoreCase(userData.name) && (checkUser.mail.equalsIgnoreCase(userData.mail))) {
        String key = application.requests.add(checkUser, PWD_REQUEST_VALID_MILLIS);
        logger.warn("Request: " + application.tammUrl + "newpwd.html?key=" + key);
        message = "Anforderung per Mail verschickt";
        found = true;
      } else {
        logger.debug("Name or Mail mismatch for " + userData.name + " and " + userData.mail);
      }
    } catch(TammError ex) {
      logger.debug("Invalid password request for " + userData.name);
    }
    
    ServletUtils.sendResult(resultData, found, "", "", message, null);
  }
  
  /**
   * Checks and stores new password.
   * 
   * @param reader
   * @param resultData
   * @throws IOException
   * @throws TammError 
   */
  private void processUpdatePassword(Reader reader, OutputStream resultData) throws IOException, TammError {
    String message = "";
    boolean found = false;
    LoginData loginData = new Gson().fromJson(reader, LoginData.class);
    UserData requestUser = application.requests.getUserItem(loginData.key);
    if (requestUser != null) {
      var updateUser = application.users.readUser(requestUser.id, null);
      updateUser.pwd = PasswordUtils.encodePassword(loginData.pwd);
      application.users.writeUser(updateUser);
      found = true;
    } else {
      message = "Ungültiger oder abgelaufener Schlüssel";
    }
    
    ServletUtils.sendResult(resultData, found, "login.html", "", message, null);
  }
  
}
