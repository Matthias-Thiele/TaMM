/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import de.mmth.tamm.data.ClientData;
import de.mmth.tamm.data.KeyValue;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.TaskData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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
   * @throws IOException 
   * @throws de.mmth.tamm.TammError 
   */
  public void process(SessionData session, String cmd, InputStream sourceData, OutputStream resultData, String cmd4) throws IOException, TammError {
    switch (cmd) {
      case "removetask":
        processRemoveTask(resultData, session, cmd4);
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
  
}
