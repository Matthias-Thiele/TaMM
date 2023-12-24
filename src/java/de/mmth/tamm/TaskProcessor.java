/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import de.mmth.tamm.data.FindData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.data.TaskData;
import de.mmth.tamm.data.UserData;
import de.mmth.tamm.utils.DateUtils;
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
public class TaskProcessor {
  private static final Logger logger = LogManager.getLogger(TaskProcessor.class);

  private final ApplicationData application;
  private final Gson gson;
  
  public TaskProcessor(ApplicationData application) {
    this.application = application;
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setLongSerializationPolicy( LongSerializationPolicy.STRING );
    gson = gsonBuilder.create();
  }
  
  protected void processSaveTask(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      ServletUtils.sendResult(resultData, false, "", "", "Sie sind noch nicht angemeldet.", null);
      return;
    }
    
    TaskData taskData = gson.fromJson(reader, TaskData.class);
    taskData.lastChanged = DateUtils.formatZ(null);
    if (taskData.createDate == null) {
      taskData.createDate = taskData.lastChanged;
      taskData.creator = session.user.id;
    }
    long lid = application.tasks.writeTask(taskData);
    logger.debug("Task written: " + lid + " : " + taskData.name);
    ServletUtils.sendResult(resultData, true, "", "", "", Long.toString(lid));
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
    
    FindData findData = gson.fromJson(reader, FindData.class);
    logger.debug("Search tasklist " + findData.filterText);
    int userId = session.user.id; 
    if (userId == 1) userId = -1; // TODO remove
    var searchResult = application.tasks.listTasks(userId, findData.filterText);
    
    try (Writer writer = new OutputStreamWriter(resultData)) {
      gson.toJson(searchResult, writer);
    }
  }
  
}
