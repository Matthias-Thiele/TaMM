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
import de.mmth.tamm.data.FindData;
import de.mmth.tamm.data.JsonResult;
import de.mmth.tamm.data.LockData;
import de.mmth.tamm.data.RoleAssignmentData;
import de.mmth.tamm.data.RoleData;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.DateUtils;
import de.mmth.tamm.utils.PasswordUtils;
import de.mmth.tamm.utils.Placeholder;
import de.mmth.tamm.utils.ServletUtils;
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
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
          
        case "domainlock":
          processDomainLock(reader, resultData, session);
          break;
          
        case "saverole":
          processSaveRole(reader, resultData, session);
          break;
          
        case "saveassignments":
          processSaveUserRoles(reader, resultData, session);
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
      String missingLogin = Txt.get(session.lang, "missing_login");
      ServletUtils.sendResult(resultData, false, "", "", missingLogin, null);
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
    String message = "";
    if ((session.user == null) || !session.user.mainAdmin) {
      message = Txt.get(session.lang, "access_denied");
    } else {
      ClientData clientData = new Gson().fromJson(reader, ClientData.class);
      logger.debug("Process login request for user" + clientData.name);
      application.clients.writeClient(clientData);
      application.refreshClientNames();
    }
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, null);
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
    boolean loginValid = false;
    if (!application.accessCache.checkAccess(session.clientIp)) {
      result.result = "error";
      result.message = Txt.get(session.lang, "account_locked");
      result.nextPage = "";
    } else {
      try {
        var user = application.users.readUser(session.client.id, -1, loginData.name);
        if (PasswordUtils.comparePassword(user.pwd, loginData.pwd)) {
          result.result = "ok";
          result.nextPage = "index.html";
          session.loginTime = DateUtils.formatZ(null); 
          session.user = user;
          session.user.pwd = ""; // do not leak password hash to the outer world.
          application.users.updateLoginDate(session.client.id, user.id, session.loginTime);
          loginValid = true;
        }
      } catch( TammError ex) {
        // loginValid stays false
      }

      if (!loginValid) {
        result.result = "error";
        result.nextPage = "";
        result.message =Txt.get(session.lang, "unknown_user");
        application.accessCache.addInvalidAccess(session.clientIp);
        logger.warn("Invalid access: " + session.clientIp + " - " + loginData.name);
      }
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
    String message = Txt.get(session.lang, "unknown_user_or_mail");
    boolean isOk = false;
    try {
      message = checkLocked(userData.mail, session.lang);
      var checkUser = application.users.readUser(session.client.id, -1, userData.name);
      if (checkUser.name.equalsIgnoreCase(userData.name) && (checkUser.mail.equalsIgnoreCase(userData.mail))) {
        String key = application.requests.add(checkUser, PWD_REQUEST_VALID_MILLIS, session.clientIp);
        String validUntil = DateUtils.formatD(application.requests.getValidDate(key));
        logger.warn("Request: " + application.tammUrl + "newpwd.html?key=" + key);
        if (application.mailer != null) {
          var requestUrl = application.tammUrl + "newpwd.html?key=" + key;
          var lockUrl = application.tammUrl + "system/lockmail/" + key;
          try {
            message = processMail(requestUrl, lockUrl, userData.mail, validUntil, session.lang);
            isOk = true;
          } catch (EmailException ex) {
            logger.warn("Error sending mail.", ex);
            message = Txt.get(session.lang, "error_send_mail");
          }
        } else {
          message = Txt.get(session.lang, "missing_mail_config");
        }
      } else {
        logger.debug("Name or Mail mismatch for " + userData.name + " and " + userData.mail);
      }
    } catch(TammError ex) {
      logger.debug("Invalid password request for " + userData.name);
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }
  
  /**
   * Checks if the mail address accepts mails.
   * 
   * Invalid requests will be counted in the lock list.
   * 
   * @param mail
   * @return null if ok, otherwiese error message
   * @throws TammError 
   */
  private String checkLocked(String mail, String lang) throws TammError {
    String message = null;
    if (application.locks.checkLock(mail)) {
      message = Txt.get(lang, "mail_locked");
      application.locks.incrementLockCount(mail);
    } else {
      String domain = mail;
      int pos = domain.indexOf('@');
      if (pos > 0) {
        domain = domain.substring(pos);
      }
      if (application.locks.checkLock(domain)) {
        message = Txt.get(lang, "mail_domain_locked");
        application.locks.incrementLockCount(domain);
      }
    }
    
    return message;
  }

  /**
   * Creates the mail and sends it per SMTP.
   * 
   * @param requestUrl
   * @param lockUrl
   * @param mailAddress
   * @param linkValidUntil
   * @param lang
   * @return
   * @throws EmailException
   * @throws TammError 
   */
  private String processMail(String requestUrl, String lockUrl, String mailAddress, String linkValidUntil, String lang) throws EmailException, TammError {
    Map<String, String> params = new HashMap<>();
    params.put("pwdrenewal", requestUrl);
    params.put("pwdmaillock", lockUrl);
    params.put("pwdexpired", linkValidUntil);
    Placeholder ph = new Placeholder();
    
    var htmlmessage = application.templates.getTemplate("mail/mailtemplate.html");
    htmlmessage = ph.resolve(htmlmessage, params);
    
    var textmessage = application.templates.getTemplate("mail/mailtemplate.txt");
    textmessage = ph.resolve(textmessage, params);
    
    String msg = Txt.get(lang, "pwd_renewal");
    application.mailer.send(application.adminData.mailreply, mailAddress, msg, textmessage, htmlmessage);
    
    return Txt.get(lang, "req_sent_per_mail");
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
    int userId = application.requests.getUserId(loginData.key);
    if (userId != -1) {
      var updateUser = application.users.readUser(session.client.id, userId, null);
      updateUser.pwd = PasswordUtils.encodePassword(loginData.pwd);
      application.users.writeUser(updateUser);
      found = true;
      application.requests.removeKey(loginData.key);
    } else {
      message = Txt.get(session.lang, "invalid_key");
    }
    
    ServletUtils.sendResult(resultData, found, "login.html", "", message, null);
  }

  /**
   * Creates a new mail address lock.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws TammError
   * @throws IOException 
   */
  private void processDomainLock(Reader reader, OutputStream resultData, SessionData session) throws TammError, IOException {
    String message = "";
    if (session.user.mainAdmin) {
      FindData findData = new Gson().fromJson(reader, FindData.class);
      var mail = findData.filterText;
      var pos = mail.indexOf('@');
      if (pos > 0) {
        mail = mail.substring(pos);
      }
      LockData lock = new LockData();
      lock.mailAddress = mail;
      lock.lockDate = DateUtils.formatZ(null);
      lock.lockIP = "admin";
      application.locks.writeLock(lock);
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
    
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, null);
  }

  /**
   * Update existing role or insert new role.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws TammError
   * @throws IOException 
   */
  private void processSaveRole(Reader reader, OutputStream resultData, SessionData session) throws TammError, IOException {
    String message = Txt.get(session.lang, "access_denied");
    Integer id = -1;
    
    if (session.user.mainAdmin || session.user.subAdmin) {
      RoleData data = new Gson().fromJson(reader, RoleData.class);
      data.clientId = session.client.id;
      data.owner = session.user.id;
      id = application.roles.writeRole(data);
      message = Txt.get(session.lang, "role_created");
    }
    
    ServletUtils.sendResult(resultData, true, "", "", message, id);
  }

  /**
   * Save assigned roles of given user.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws TammError
   * @throws IOException 
   */
  private void processSaveUserRoles(Reader reader, OutputStream resultData, SessionData session) throws TammError, IOException {
    String message = Txt.get(session.lang, "access_denied");
    
    if (session.user.mainAdmin || session.user.subAdmin) {
      RoleAssignmentData data = new Gson().fromJson(reader, RoleAssignmentData.class);
      application.assignments.writeRoleAssignments(data);
      message = "";
    }
    
    ServletUtils.sendResult(resultData, true, "", "", message, null);
  }
  
}
