/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.LockData;
import de.mmth.tamm.data.RoleAssignmentData;
import de.mmth.tamm.data.RoleData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.utils.DateUtils;
import de.mmth.tamm.utils.Placeholder;
import de.mmth.tamm.utils.RequestCache;
import de.mmth.tamm.utils.ServletUtils;
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.mail.EmailException;
import org.apache.logging.log4j.Logger;

/**
 * Processes all incomming GET requests.
 * 
 * @author matthias
 */
public class GetProcessor {
  private static final Logger logger = TammLogger.prepareLogger(GetProcessor.class);
  
  private final ApplicationData application;
  
  /**
   * Constructor with global application data.
   * 
   * @param application 
   */
  public GetProcessor(ApplicationData application) {
    this.application = application;
  }
  
  /**
   * Process incoming post request.
   * 
   * @param session
   * @param cmd
   * @param sourceData
   * @param resultData
   * @param cmd4
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String cmd, InputStream sourceData, OutputStream resultData, String cmd4) throws IOException, TammError {
    switch (cmd) {
      case "session":
        processSession(resultData, session);
        break;

      case "logout":
        processLogout(resultData, session);
        break;
        
      case "clientlist":
        processClientList(resultData, session);
        break;
        
      case "attachments":
        processAttachmentsList(resultData, session, cmd4);
        break;
        
      case "initdata":
        processInitdata(resultData, session);
        break;
        
      case "lockmail":
        processLock(resultData, session, cmd4);
        break;
        
      case "locklist":
        processLockList(resultData, session, cmd4);
        break;
        
      case "pwreq":
        processPasswordRequestList(resultData, session);
        break;
        
      case "roleslist":
        processRolesList(resultData, session, cmd4);
        break;
        
      case "assignmentslist":
        processAssignmentsList(resultData, session, cmd4);
        break;
        
      case "invitation":
        processInvitation(resultData, session, cmd4);
        break;
    }
  }
  
  /**
   * User request to add his mail address to the lock list.
   * @param key 
   */
  private void processLock(OutputStream resultData, SessionData session, String key) throws TammError, IOException {
    String message;
    logger.info(key);
    String userMail = application.requests.getUserMail(key);
    if (userMail != null) {
      LockData lock = new LockData();
      lock.mailAddress = userMail;
      lock.lockDate = DateUtils.formatZ(null);
      application.locks.writeLock(lock);
      application.requests.removeKey(key);
      message = Txt.get(session.lang, "mail_address_locked");
    } else {
      message = Txt.get(session.lang, "pwd_req_expired");
    }
    
    resultData.write(message.getBytes(StandardCharsets.UTF_8));
  }
  
  /**
   * Sends the actual session data.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  private void processSession(OutputStream resultData, SessionData session) throws IOException, TammError {
    String message = "";
    boolean isOk = session.user != null;
    if (isOk) {
      ClientData client = session.client;
      List<KeyValue> userNames = application.userNamesMap.get(client.name);
      if (userNames == null) {
        userNames = application.users.listUserNames(client.id);
        application.userNamesMap.put(client.name, userNames);
      }
      session.userNames = userNames;
      
      List<KeyValue> roleNames = application.roleNamesMap.get(client.name);
      if (roleNames == null) {
        roleNames = new ArrayList<>();
        var roles = application.roles.listRoles(client.id, -1);
        for (var role: roles) {
          var kv = new KeyValue(role.id, role.name);
          roleNames.add(kv);
        }
        application.roleNamesMap.put(client.name, roleNames);
      }
      session.roleNames = roleNames;
    } else {
      message = Txt.get(session.lang, "missing_login");
    }
    
    String errorPage = application.db.isValid() ? "login.html" : "admin.html";
    ServletUtils.sendResult(resultData, isOk, "", errorPage, message, session);
  }
  
  /**
   * Deletes the actual session.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  private void processLogout(OutputStream resultData, SessionData session) throws IOException, TammError {
    session.user = null;
    ServletUtils.sendResult(resultData, true, "login.html", "", "", session);
  }
  
  /**
   * Returns a list of configured clients.
   * 
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  private void processClientList(OutputStream resultData, SessionData session) throws IOException, TammError {
    boolean isUser = session.user != null;
    String message = "";
    String errorNext = "";
    if (isUser) {
      if (!session.user.mainAdmin) {
        message = Txt.get(session.lang, "missing_access_rights");
      }
    } else {
      message = Txt.get(session.lang, "missing_login");
      errorNext = "login.html";
    }
    
    ServletUtils.sendResult(resultData, isUser, "", errorNext, message, isUser ? application.clientList : null);
  }
  
  /**
   * Returns a list of file or url attachments of the given task.
   * 
   * @param resultData
   * @param session
   * @param param task id
   * @throws IOException
   * @throws TammError 
   */
  private void processAttachmentsList(OutputStream resultData, SessionData session, String param) throws IOException, TammError {
    var attachmentList = application.attachments.listAttachments(session.client.id, Long.parseLong(param));
    ServletUtils.sendResult(resultData, true, "", "", "", attachmentList);
  }

  /**
   * Returns the admin data object.
   * 
   * null if not main administrator. This is not an error.
   * @param resultData
   * @param session
   * @throws IOException 
   */
  private void processInitdata(OutputStream resultData, SessionData session) throws IOException {
    AdminData data = null;
    if ((session.user != null) && session.user.mainAdmin) {
      data = application.adminData;
    }
    
    ServletUtils.sendResult(resultData, true, "", "", "", data);
  }

  /**
   * Returns a list of locked mail addresses.
   * 
   * cmd4 contains a filter. SQL wildcard characters are allowed.
   * 
   * @param resultData
   * @param session
   * @param cmd4 filter text
   * @throws TammError
   * @throws IOException 
   */
  private void processLockList(OutputStream resultData, SessionData session, String cmd4) throws TammError, IOException {
    List<LockData> result = null;
    String message = "";
    if ((session.user == null) || !session.user.mainAdmin) {
      message = Txt.get(session.lang, "access_denied");
    } else {
      result = application.locks.listLocks(cmd4);
    }
    
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, result);
  }

  /**
   * Sends the password request list.
   * 
   * @param resultData
   * @param session
   * @throws IOException 
   */
  private void processPasswordRequestList(OutputStream resultData, SessionData session) throws IOException {
    String message = "";
    RequestCache requests = null;
    
    if ((session.user == null) || !session.user.mainAdmin) {
      message = Txt.get(session.lang, "access_denied");
    } else {
      requests = application.requests;
    }
    
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", "", requests);
  }

  /**
   * Sends a list of all roles of the current user.
   * 
   * Only main admins and sub admins are allowed.
   * main admins can read all roles with parameter "all"
   * 
   * @param resultData
   * @param session
   * @param cmd4 empty or "all"
   * @throws TammError
   * @throws IOException 
   */
  private void processRolesList(OutputStream resultData, SessionData session, String cmd4) throws TammError, IOException {
    List<RoleData> result = null;
    String message = "";
    if (session.user == null) {
      message = Txt.get(session.lang, "access_denied");
    } else {
      int owner = (cmd4.equals("all")) ? -1 : session.user.id;
      result = application.roles.listRoles(session.client.id, owner);
    }
    
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, result);
  }

  /**
   * Returns the assigned roles of the given user.
   * 
   * @param resultData
   * @param session
   * @param cmd4 userId as String
   * @throws TammError
   * @throws IOException 
   */
  private void processAssignmentsList(OutputStream resultData, SessionData session, String cmd4) throws TammError, IOException {
    RoleAssignmentData result = null;
    String message = "";
    if (session.user == null) {
      message = Txt.get(session.lang, "access_denied");
    } else {
      int userId = Integer.parseInt(cmd4);
      result = application.assignments.listRoleAssignments(userId);
      result.user = userId;
    }
    
    ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, result);
  }

  private void processInvitation(OutputStream resultData, SessionData session, String cmd4) {
    String lang = session.lang;
    
    try {
      var checkUser = application.users.readUser(session.client.id, Integer.parseInt(cmd4), null);
      String lockInfo = ServletUtils.checkLocked(application, checkUser.mail, lang);
      
      String message = "";
      if (lockInfo == null) {
        if (application.mailer != null) {
          String key = application.requests.add(checkUser, application.adminData.pwdreqvaildhours * 3600000, session.clientIp);
          String validUntil = DateUtils.formatD(application.requests.getValidDate(key));
          var requestUrl = application.tammUrl + "pwdreq.html";
          var lockUrl = application.tammUrl + "system/lockmail/" + key;
          try {
            Map<String, String> params = new HashMap<>();
            params.put("pwdrenewal", requestUrl);
            params.put("pwdmaillock", lockUrl);
            params.put("pwdexpired", validUntil);
            params.put("newusername", checkUser.name);
            params.put("newusermail", checkUser.mail);
            Placeholder ph = new Placeholder();

            var htmlmessage = application.templates.getTemplate("mail/" + lang + "_invitation.html");
            htmlmessage = ph.resolve(htmlmessage, params);

            var textmessage = application.templates.getTemplate("mail/" + lang + "_invitation.txt");
            textmessage = ph.resolve(textmessage, params);

            String msg = Txt.get(lang, "subject_invitation");
            application.mailer.send(application.adminData.mailreply, checkUser.mail, msg, textmessage, htmlmessage);

            message = Txt.get(lang, "req_sent_per_mail");
          } catch (EmailException ex) {
            logger.warn("Error sending mail.", ex);
            message = Txt.get(lang, "error_send_mail");
          }
          
        }
      } else {
        message = Txt.get(lang, "mail_locked");
      }
      
      ServletUtils.sendResult(resultData, true, "", "", message, null);
    } catch(TammError | IOException | NumberFormatException ex) {
      logger.debug("Invalid send invitation mail for user id: " + cmd4);
    }
  }
  
}
