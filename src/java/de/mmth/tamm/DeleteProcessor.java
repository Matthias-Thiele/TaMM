/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import com.google.gson.Gson;
import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.DeleteData;
import de.mmth.tamm.data.LockData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.TaskData;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.CleanType;
import de.mmth.tamm.utils.DBUtils;
import de.mmth.tamm.utils.ServletUtils;
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class DeleteProcessor {
  private static final Logger logger = TammLogger.prepareLogger(DeleteProcessor.class);
  
  private final ApplicationData application;
  
  /**
   * Constructor with global application data.
   * 
   * @param application 
   */
  public DeleteProcessor(ApplicationData application) {
    this.application = application;
  }
  
  /**
   * Process incoming delete request.
   * 
   * @param session
   * @param cmd
   * @param sourceData
   * @param resultData
   * @param cmd4
   * @param cmd5
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String cmd, InputStream sourceData, OutputStream resultData, String cmd4, String cmd5) throws IOException, TammError {
    logger.info("Delete " + cmd);
    switch (cmd) {
      case "removetask":
        processRemoveTask(resultData, session, cmd4);
        break;
        
      case "deletelock":
        processDeleteLock(resultData, session, cmd4, cmd5);
        break;
        
      case "cleantemplates":
        processCleanTemplates(resultData, session);
        break;
        
      case "deletereq":
        processDeleteRequest(resultData, session, cmd4);
        break;
        
      case "deleterole":
        processDeleteRole(resultData, session, cmd4);
        break;
        
      case "cleaniplocks":
        processCleanLocks(resultData, session, CleanType.IP_LOCKS);
        break;
        
      case "cleanmaillocks":
        processCleanLocks(resultData, session, CleanType.MAIL_LOCKS);
        break;
        
      case "deleteuser":
        processDeleteUser(resultData, session, sourceData);
        break;
    }
  }
  
  /**
   * Deletes the given task.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  private void processRemoveTask(OutputStream resultData, SessionData session, String cmd4) throws IOException, TammError {
    boolean isOk = session.user != null;
    if (isOk) {
      ClientData client = session.client;
      TaskData task = application.tasks.readTask(client.id, Long.parseLong(cmd4));
      if (task.owner == session.user.id) {
        application.tasks.removeTask(task);
      }
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", "", null);
  }
  
  /**
   * Deletes the given mail address lock.
   * 
   * One mail address can have several locks, for deletion 
   * there has to be the address and the timestamp.
   * @param resultData
   * @param session
   * @param cmd4 mail address
   * @param cmd5 timestamp
   * @throws IOException
   * @throws TammError 
   */
  private void processDeleteLock(OutputStream resultData, SessionData session, String cmd4, String cmd5) throws IOException, TammError {
    boolean isOk = (session.user != null) && session.user.mainAdmin;
    String message = "";
    
    if (isOk) {
      LockData lock = new LockData();
      lock.mailAddress = cmd4;
      lock.lockDate = cmd5;
      application.locks.removeLock(lock);
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }

  private void processCleanTemplates(OutputStream resultData, SessionData session) throws IOException {
    boolean isOk = (session.user != null) && session.user.mainAdmin;
    
    String message;
    if (isOk) {
      int count = application.templates.clear();
      message = application.placeholder.resolve(Txt.get(session.lang, "messages_deleted"), "count", Integer.toString(count));
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }

  /**
   * Removes the password request with the given key.
   * 
   * @param resultData
   * @param session
   * @param cmd4
   * @throws IOException 
   */
  private void processDeleteRequest(OutputStream resultData, SessionData session, String cmd4) throws IOException {
    boolean isOk = (session.user != null) && session.user.mainAdmin;
    
    String message;
    if (isOk) {
      application.requests.removeKey(cmd4);
      message = Txt.get(session.lang, "pdw_request_deleted") + cmd4;
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }

  /**
   * Deletes a role from the list of roles.
   * 
   * @param resultData
   * @param session
   * @param cmd4
   * @throws TammError
   * @throws IOException 
   */
  private void processDeleteRole(OutputStream resultData, SessionData session, String cmd4) throws TammError, IOException {
    boolean isOk = (session.user != null) && (session.user.mainAdmin || session.user.subAdmin);
    String message;
    if (isOk) {
      int roleId = Integer.parseInt(cmd4);
      application.roles.removeRole(session.client.id, roleId);
      application.assignments.clearRoleAssignments(roleId, false);
      message = Txt.get(session.lang, "role_deleted");
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
      
    application.roleNamesMap.remove(session.client.name);
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }

  /**
   * Deletes all IP address locks.
   * 
   * @param resultData
   * @param session
   * @throws IOException 
   */
  private void processCleanLocks(OutputStream resultData, SessionData session, CleanType kind) throws IOException {
    boolean isOk = (session.user != null) && session.user.mainAdmin;
    
    String message = "";
    if (isOk) {
      switch (kind) {
        case IP_LOCKS:
          int count = application.accessCache.clear();
          message = application.placeholder.resolve(Txt.get(session.lang, "iplocks_deleted"), "count", Integer.toString(count));
          break;
          
        case MAIL_LOCKS:
          int mailcount = application.mailCounter.cleanup();
          message = application.placeholder.resolve(Txt.get(session.lang, "maillocks_deleted"), "count", Integer.toString(mailcount));
          break;
      }
    } else {
      message = Txt.get(session.lang, "access_denied");
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "", message, null);
  }

  // ToDo: check rights
  private void processDeleteUser(OutputStream resultData, SessionData session, InputStream sourceData) throws IOException, TammError {
    String message;
    try (Reader reader = new InputStreamReader(sourceData)) {
      DeleteData data = new Gson().fromJson(reader, DeleteData.class);
      logger.warn("Process delete user request for " + data.deleteId);
      var user = application.users.readUser(data.clientId, data.deleteId, null);
      if (user == null) {
        message = Txt.get(session.lang, "user_not_found");
      } else {
        if (data.substituteId > 0) {
          boolean substExists = existsUserOrRole(user.clientId, data.substituteId);
          if (substExists) {
            message = doDelete(user, data.substituteId);
          } else {
            message = Txt.get(session.lang, "substitute_not_found");
          }
        } else {
          message = doDelete(user, -1);
        }
      }
      
      ServletUtils.sendResult(resultData, message.isEmpty(), "", "", message, null);
    }
  }
  
  /**
   * Checks if the given userRoleId exists as an user or a role.
   * 
   * @param clientId
   * @param userRoleId
   * @return
   * @throws TammError 
   */
  private boolean existsUserOrRole(int clientId, int userRoleId) {
    try {
      if (userRoleId > 1000000) {
        var roleClientId = application.roles.getRoleClient(userRoleId);
        return roleClientId > 0;
      } else {
          var user = application.users.readUser(clientId, userRoleId, null);
          return user != null;
      }
    } catch (TammError ex) {
      return false;
    }
  }
  
  /**
   * Delete the given user.
   * If substitute is given a valid id, then move all tasks
   * of this user to the substitute. Otherwise delete all tasks
   * of this user.
   * 
   * @param user
   * @param substitute
   * @return
   * @throws TammError 
   */
  private String doDelete(UserData user, int substitute) throws TammError {
    var message = "";
    if (substitute > 0) {
      application.tasks.moveTasksOwner(user.clientId, user.id, substitute);
    } else {
      int count = DBUtils.deleteAttachmentsOfUser(application.db, user.id, application.tasks.getTableName(), application.attachments.getTableName());
      logger.info("Result of delete attachments: " + count);
      
      application.tasks.deleteTasksOfOwner(user.clientId, user.id);
    }
    
    application.users.deleteUser(user.clientId, user.id);
    return message;
  }
}
