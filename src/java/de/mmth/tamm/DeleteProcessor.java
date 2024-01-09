/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.LockData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.TaskData;
import de.mmth.tamm.utils.CleanType;
import de.mmth.tamm.utils.ServletUtils;
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class DeleteProcessor {
  private static final Logger logger = LogManager.getLogger(DeleteProcessor.class);
  
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
    logger.info("Process " + cmd);
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
  
}
