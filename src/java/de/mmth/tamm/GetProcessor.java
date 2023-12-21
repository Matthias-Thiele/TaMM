/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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
      logger.warn("Missing initialisation data, request aborted.");
      ServletUtils.gotoErrorPage(resultData);
      return;
    }
    
    switch (uriParts[3]) {
      case "session":
        processSession(resultData, session);
        break;

      case "logout":
        processLogout(resultData, session);
        break;
    }
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
    boolean isOk = session.user != null;
    ServletUtils.sendResult(resultData, isOk, "", "login.html", "", session);
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
  
}
