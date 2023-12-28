/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import com.google.gson.Gson;
import de.mmth.tamm.data.FindData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.PasswordUtils;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
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
public class UserProcessor {
  private static final Logger logger = LogManager.getLogger(UserProcessor.class);

  private final ApplicationData application;
  
  public UserProcessor(ApplicationData application) {
    this.application = application;
  }
  
  /**
   * Processes a filter request from the web page.
   * 
   * returns a list of found entries.
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  protected void processFilter(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      throw new TammError("Missing login.");
    }
    
    FindData findData = new Gson().fromJson(reader, FindData.class);
    logger.debug("Search userlist " + findData.filterText);
    int userId = session.user.id; 
    if (userId == 1) userId = -1; // TODO remove
    int clientId = (session.user.mainAdmin) ? -1 : session.client.id;
    var searchResult = application.users.listUsers(clientId, userId, findData.filterText, findData.onlyAdmins);
    for (UserData user: searchResult) {
      user.pwd = ""; // do not leak user passwords to the outside.
    }
    
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(searchResult, writer);
    }
  }
  
  /**
   * Insert or update given user data.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  protected void processSaveUser(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      ServletUtils.sendResult(resultData, false, "", "", "Sie sind noch nicht angemeldet.", null);
      return;
    }
    
    UserData userData = new Gson().fromJson(reader, UserData.class);
    if (!session.user.mainAdmin) {
      userData.mainAdmin = false;
    }
    
    if (!session.user.subAdmin && !session.user.mainAdmin) {
      userData.subAdmin = false;
    }
    
    userData.administratorId = session.user.id;
    if (!session.user.mainAdmin || userData.clientId == 0) {
      userData.clientId = session.client.id;
    }
    
    String errorMsg = "";
    if (userData.id == -1) {
      // new user
     logger.info("Insert user data for user" + userData.name);
      userData.pwd = PasswordUtils.encodePassword(Long.toHexString((long)(Math.random() * Long.MAX_VALUE)));
      application.users.writeUser(userData);
    } else {
      // update existing user
      logger.info("Update user data for user" + userData.name);
      var checkUser = application.users.readUser(session.client.id, userData.id, null);
      if (checkUser.administratorId == 0) {
        checkUser.administratorId = checkUser.id;
      }
      
      if (checkUser.administratorId != session.user.id) {
        errorMsg = "Es können nur eigene Anwender bearbeitet werden.";
        logger.warn("Invalid user access from " + session.user.name + " to user " + userData.name);
      } else {
        checkUser.name = userData.name;
        checkUser.mail = userData.mail;
        checkUser.mainAdmin = userData.mainAdmin;
        checkUser.subAdmin = userData.subAdmin;
        if (session.user.mainAdmin) {
          checkUser.clientId = userData.clientId;
        }
        application.users.writeUser(checkUser);
      }
    }
    
    ServletUtils.sendResult(resultData, errorMsg.isBlank(), "", "", errorMsg, null);
  }  
  
}
