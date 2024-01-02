/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.LockData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.DateUtils;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Processes all incomming GET requests.
 * 
 * @author matthias
 */
public class GetProcessor {
  private static final Logger logger = LogManager.getLogger(GetProcessor.class);
  
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
        processLock(resultData, cmd4);
        break;
        
      case "locklist":
        processLockList(resultData, session, cmd4);
    }
  }
  
  /**
   * User request to add his mail address to the lock list.
   * @param key 
   */
  private void processLock(OutputStream resultData, String key) throws TammError, IOException {
    String message;
    logger.info(key);
    String userMail = application.requests.getUserMail(key);
    if (userMail != null) {
      LockData lock = new LockData();
      lock.mailAddress = userMail;
      lock.lockDate = DateUtils.formatZ(null);
      application.locks.writeLock(lock);
      application.requests.removeKey(key);
      message = "Die Mail Adresse wurde in die Sperrliste aufgenommen.";
    } else {
      message = "Der Zugriffsschlüssel ist bereits abgelaufen.";
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
      List<KeyValue> clientNames = application.userNamesMap.get(client.name);
      if (clientNames == null) {
        clientNames = application.users.listUserNames(client.id);
        application.userNamesMap.put(client.name, clientNames);
      }
      session.userNames = clientNames;
    } else {
      message = "Anmeldung fehlt.";
    }
    
    ServletUtils.sendResult(resultData, isOk, "", "login.html", message, session);
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
        message = "Keine Berechtigung.";
      }
    } else {
      message = "Keine Anmeldung.";
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
    List<LockData> result = application.locks.listLocks(cmd4);
    ServletUtils.sendResult(resultData, true, "", "", "", result);
  }
  
}
