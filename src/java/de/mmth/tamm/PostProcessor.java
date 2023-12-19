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
    if (user.pwd.equals(loginData.pwd)) {
      result.result = "ok";
      result.nextPage = "index.html";
      session.loginTime = new Date();
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
    
    JsonResult result = new JsonResult();
    if (application.checkInit()) {
      result.result = "ok";
      result.nextPage = "index.html";
    } else {
      result.result = "error";
      result.nextPage = "admin.html";
    }
    try (Writer writer = new OutputStreamWriter(resultData)) {
      new Gson().toJson(result, writer);
    }
  }
}
