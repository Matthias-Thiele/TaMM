/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.LoginData;
import de.mmth.tamm.data.SessionData;
import com.google.gson.Gson;
import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.AttachmentData;
import de.mmth.tamm.data.ClientData;
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
import java.util.UUID;
import org.apache.commons.mail.EmailException;
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
  private final TaskProcessor taskProcessor;
  
  /**
   * Constructor with global application data.
   * 
   * @param application 
   */
  public PostProcessor(ApplicationData application) {
    this.application = application;
    this.userProcessor = new UserProcessor(application);
    this.taskProcessor = new TaskProcessor(application);
  }
  
  /**
   * Process incoming post request.
   * 
   * @param session
   * @param cmd
   * @param sourceData
   * @param resultData
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String cmd, InputStream sourceData, OutputStream resultData) throws IOException, TammError {
    try (Reader reader = new InputStreamReader(sourceData)) {
      switch (cmd) {
        case "login":
          processLogin(reader, resultData, session);
          break;
          
        case "initdata":
          processInitdata(reader, resultData, session);
          break;
          
        case "filteruser":
          userProcessor.processFilter(reader, resultData, session);
          break;
          
        case "saveuser":
          userProcessor.processSaveUser(reader, resultData, session);
          break;
          
        case "pwdreq":
          processPasswordRequest(reader, resultData, session);
          break;
          
        case "updatepwd":
          processUpdatePassword(reader, resultData, session);
          break;
          
        case "savetask":
          taskProcessor.processSaveTask(reader, resultData, session);
          break;
          
        case "filtertask":
          taskProcessor.processFilter(reader, resultData, session);
          break;
          
        case "advancetask":
          taskProcessor.processAdvance(reader, resultData, session);
          break;
          
        case "saveclient":
          processSaveClient(reader, resultData, session);
          break;
          
        case "saveurl":
          processSaveUrlAttachment(reader, resultData, session);
          break;
      }
    }
  }
  
  /**
   * Write an URL Attachment data block into the database.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  protected void processSaveUrlAttachment(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      ServletUtils.sendResult(resultData, false, "", "", "Sie sind noch nicht angemeldet.", null);
      return;
    }
    AttachmentData data = new Gson().fromJson(reader, AttachmentData.class);
    data.clientId = session.client.id;
    data.guid = UUID.randomUUID().toString();
    application.attachments.writeAttachment(data);
    
    ServletUtils.sendResult(resultData, true, "", "", data.guid, null);
  }
  
  /**
   * Writes the client info into the database.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  private void processSaveClient(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    ClientData clientData = new Gson().fromJson(reader, ClientData.class);
    logger.debug("Process login request for user" + clientData.name);
    application.clients.writeClient(clientData);
    application.refreshClientNames();
    ServletUtils.sendResult(resultData, true, "", "", "", null);
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
    var user = application.users.readUser(session.client.id, -1, loginData.name);
    if (PasswordUtils.comparePassword(user.pwd, loginData.pwd)) {
      result.result = "ok";
      result.nextPage = "index.html";
      session.loginTime = DateUtils.formatZ(null); 
      session.user = user;
      session.user.pwd = ""; // do not leak password.
      application.users.updateLoginDate(session.client.id, user.id, session.loginTime);
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
  private void processPasswordRequest(Reader reader, OutputStream resultData, SessionData session) throws IOException {
    UserData userData = new Gson().fromJson(reader, UserData.class);
    logger.debug("Password request for user " + userData.name);
    boolean found = false;
    String message = "Unbekannter Anwender oder Mailadresse";
    try {
      var checkUser = application.users.readUser(session.client.id, -1, userData.name);
      if (checkUser.name.equalsIgnoreCase(userData.name) && (checkUser.mail.equalsIgnoreCase(userData.mail))) {
        String key = application.requests.add(checkUser, PWD_REQUEST_VALID_MILLIS);
        logger.warn("Request: " + application.tammUrl + "newpwd.html?key=" + key);
        if (application.mailer != null) {
          message = "Anforderung per Mail verschickt.";
          var requestUrl = application.tammUrl + "newpwd.html?key=" + key;
          var lockUrl = application.tammUrl + "system/lockmail/" + key;
          var htmlmessage = new StringBuilder(); 
          htmlmessage.append("<html><body><p>Es wurde ein neues Passwort für Ihren Zugang zum TaMM System angefordert.</p>");
          htmlmessage.append("<p>Sie können es über <a href='" );
          htmlmessage.append(requestUrl);
          htmlmessage.append("'>diesen Link</a> einfügen.</p><p>Falls Sie es nicht selber angefordert haben und in Zukunft keine weiteren Mails ");
          htmlmessage.append("empfangen wollen, können Sie Ihre Mailadresse über <a href='");
          htmlmessage.append(lockUrl);
          htmlmessage.append("'>diesen Link</a> sperren.");
          var textmessage = new StringBuilder(); 
          textmessage.append("Es wurde ein neues Passwort für Ihren Zugang zum TaMM System angefordert.\r\n");
          textmessage.append("Sie können es über diesen Link einfügen:\r\n" );
          textmessage.append(requestUrl);
          textmessage.append("\r\n.Falls Sie es nicht selber angefordert haben und in Zukunft keine weiteren Mails\r\n");
          textmessage.append("empfangen wollen, können Sie Ihre Mailadresse über diesen Link sperren:\r\n");
          textmessage.append(lockUrl);
          try {
            application.mailer.send(application.adminData.mailreply, userData.mail, "Passwort erneuern", textmessage.toString(), htmlmessage.toString());
            found = true;
          } catch (EmailException ex) {
            logger.warn("Error sending mail.", ex);
            message = "Fehler beim Versenden der Mail.";
          }
        } else {
          message = "Fehlende Mail-Konfiguration. Anforderung im Log verzeichnet.";
        }
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
  private void processUpdatePassword(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    String message = "";
    boolean found = false;
    LoginData loginData = new Gson().fromJson(reader, LoginData.class);
    UserData requestUser = application.requests.getUserItem(loginData.key);
    if (requestUser != null) {
      var updateUser = application.users.readUser(session.client.id, requestUser.id, null);
      updateUser.pwd = PasswordUtils.encodePassword(loginData.pwd);
      application.users.writeUser(updateUser);
      found = true;
    } else {
      message = "Ungültiger oder abgelaufener Schlüssel";
    }
    
    ServletUtils.sendResult(resultData, found, "login.html", "", message, null);
  }
  
}
