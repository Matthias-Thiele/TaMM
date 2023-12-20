/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.LoginData;
import de.mmth.tamm.data.SessionData;
import com.google.gson.Gson;
import de.mmth.tamm.data.AdminData;
import de.mmth.tamm.data.FindData;
import de.mmth.tamm.data.JsonResult;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;

/**
 *
 * @author matthias
 */
public class PostProcessor {

  private final ApplicationData application;
  
  /**
   * Constructor with global application data.
   * 
   * @param application 
   */
  public PostProcessor(ApplicationData application) {
    this.application = application;
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
      gotoErrorPage(resultData);
      return;
    }
    
    try (Reader reader = new InputStreamReader(sourceData)) {
      switch (uriParts[3]) {
        case "login":
          processLogin(reader, resultData, session);
          break;
          
        case "initdata":
          processInitdata(reader, resultData);
          break;
          
        case "filter":
          processFilter(reader, resultData, session);
          break;
          
        case "saveuser":
          processSaveUser(reader, resultData, session);
          break;
      }
    }
  }
  
  /**
   * Send result with error marker and link to error page.
   * 
   * @param resultData
   * @throws IOException 
   */
  private void gotoErrorPage(OutputStream resultData) throws IOException {
    JsonResult result = new JsonResult();
    result.result = "ok";
    result.nextPage = "error.html";
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(result, writer);
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
    JsonResult result = new JsonResult();
    try {
    var user = application.users.readUser(-1, loginData.name);
    if (ServletUtils.comparePassword(user.pwd, loginData.pwd)) {
      result.result = "ok";
      result.nextPage = "index.html";
      session.loginTime = new Date();
      session.user = user;
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
  private void processInitdata(Reader reader, OutputStream resultData) throws IOException {
    AdminData adminData = new Gson().fromJson(reader, AdminData.class);
    application.setAdminData(adminData);
    ServletUtils.sendResult(resultData, application.checkInit(), "index.html", "admin.html", "");
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
  private void processFilter(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      throw new TammError("Missing login.");
    }
    
    FindData findData = new Gson().fromJson(reader, FindData.class);
    int userId = session.user.id; 
    if (userId == 1) userId = -1; // TODO remove
    var searchResult = application.users.listUsers(userId, findData.filterText);
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
  private void processSaveUser(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      ServletUtils.sendResult(resultData, false, "", "", "Sie sind noch nicht angemeldet.");
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
    
    String errorMsg = "";
    if (userData.id == -1) {
      // new user
      userData.pwd = ServletUtils.encodePassword(Long.toHexString((long)(Math.random() * Long.MAX_VALUE)));
      application.users.writeUser(userData);
    } else {
      // update existing user
      var checkUser = application.users.readUser(userData.id, null);
      if (checkUser.administratorId == 0) {
        checkUser.administratorId = checkUser.id;
      }
      
      if (checkUser.administratorId != session.user.id) {
        errorMsg = "Es können nur eigene Anwender bearbeitet werden.";
      } else {
        checkUser.name = userData.name;
        checkUser.mail = userData.mail;
        checkUser.mainAdmin = userData.mainAdmin;
        checkUser.subAdmin = userData.subAdmin;
        application.users.writeUser(checkUser);
      }
    }
    
    ServletUtils.sendResult(resultData, errorMsg.isBlank(), "", "", errorMsg);
  }  
  
}
