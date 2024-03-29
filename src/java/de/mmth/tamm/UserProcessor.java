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
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class UserProcessor {
  private static final Logger logger = TammLogger.prepareLogger(UserProcessor.class);

  private final ApplicationData application;
  
  /**
   * Receives and stores the application object.
   * @param application 
   */
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
      String msg = Txt.get(session.lang, "missing_login");
      throw new TammError(msg);
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
    
    ServletUtils.sendResult(resultData, true, "", "", "", searchResult);
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
      String msg = Txt.get(session.lang, "missing_login");
      ServletUtils.sendResult(resultData, false, "", "", msg, null);
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
    Integer userId = -1;
    if (userData.id == -1) {
      int userCount = application.users.getUserCount(session.client.id);
      if (userCount < session.client.maxUser) {
        // new user
        logger.info("Insert user data for user" + userData.name);
        userData.pwd = PasswordUtils.encodePassword(Long.toHexString((long)(Math.random() * Long.MAX_VALUE)));
        userId = application.users.writeUser(userData);
      } else {
        errorMsg = Txt.get(session.lang, "user_count_exceeded");
      }
    } else {
      // update existing user
      logger.info("Update user data for user" + userData.name);
      var checkUser = application.users.readUser(userData.clientId, userData.id, null);
      if (checkUser.administratorId == 0) {
        checkUser.administratorId = checkUser.id;
      }
      
      if (checkUser.administratorId != session.user.id) {
        errorMsg = Txt.get(session.lang, "wrong_administrator");
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
        userId = checkUser.id;
      }
    }
      
    application.userNamesMap.remove(session.client.name);
    ServletUtils.sendResult(resultData, errorMsg.isBlank(), "", "", errorMsg, userId);
  }  
  
}
