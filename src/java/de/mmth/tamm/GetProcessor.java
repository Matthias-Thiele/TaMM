/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String cmd, InputStream sourceData, OutputStream resultData) throws IOException, TammError {
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
    ClientData client = session.client;
    List<KeyValue> clientNames = application.userNamesMap.get(client.name);
    if (clientNames == null) {
      clientNames = application.users.listUserNames(client.id);
      application.userNamesMap.put(client.name, clientNames);
    }
    session.userNames = clientNames;
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
  
  private void processClientList(OutputStream resultData, SessionData session) throws IOException, TammError {
    ServletUtils.sendResult(resultData, true, "", "", "", application.clientList);
  }
  
}
