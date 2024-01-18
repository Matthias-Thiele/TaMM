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
import de.mmth.tamm.progress.Interval;
import de.mmth.tamm.utils.DateUtils;
import de.mmth.tamm.utils.ServletUtils;
import de.mmth.tamm.utils.Txt;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class TaskProcessor {
  private static final Logger logger = TammLogger.prepareLogger(TaskProcessor.class);

  private final ApplicationData application;
  private final Gson gson;
  
  /**
   * Create and initialize Gson, dependency injection of the application object.
   * 
   * @param application 
   */
  public TaskProcessor(ApplicationData application) {
    this.application = application;
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setLongSerializationPolicy( LongSerializationPolicy.STRING );
    gson = gsonBuilder.create();
  }
  
  /**
   * Stores the task data from the task.html form into the database.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws IOException
   * @throws TammError 
   */
  protected void processSaveTask(Reader reader, OutputStream resultData, SessionData session) throws IOException, TammError {
    if (session.user == null) {
      String msg = Txt.get(session.lang, "missing_login");
      ServletUtils.sendResult(resultData, false, "", "", msg, null);
      return;
    }
    
    TaskData taskData = gson.fromJson(reader, TaskData.class);
    taskData.lastChanged = DateUtils.formatZ(null);
    if (taskData.createDate == null) {
      taskData.createDate = taskData.lastChanged;
      taskData.creator = session.user.id;
    }
    
    if (taskData.owner < 1) {
      taskData.owner = session.client.id;
    }
    
    if (taskData.nextDueDate.isBlank()) {
      taskData.nextDueDate = nextDueDate(taskData);
    }
    taskData.clientId = session.client.id;
    long lid = application.tasks.writeTask(taskData, false);
    logger.debug("Task written: " + lid + " : " + taskData.name);
    ServletUtils.sendResult(resultData, true, "", "", "", Long.toString(lid));
  }

  /**
   * Processes a task list request from the web page.
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
    
    FindData findData = gson.fromJson(reader, FindData.class);
    logger.debug("Search tasklist " + findData.filterText);
    int userId = findData.userId; 
    
    List<TaskData> searchResult;
    switch (findData.source) {
      case "tasklist":
        searchResult = application.tasks.listTasks(session.client.id, userId, findData.filterText, findData.withRoleTasks);
        break;
    
      case "historylist":
        long taskId = findData.filterText.isBlank() ? -1 : Long.parseLong(findData.filterText); 
        searchResult = application.history.listTasks(session.client.id, taskId);
        break;
        
      default:
        searchResult = new ArrayList<>();
        break;
    }

    try (Writer writer = new OutputStreamWriter(resultData)) {
      gson.toJson(searchResult, writer);
    }
  }

  
  /**
   * Advance selected task to next date after today.
   * 
   * @param reader
   * @param resultData
   * @param session
   * @throws TammError
   * @throws IOException 
   */
  void processAdvance(Reader reader, OutputStream resultData, SessionData session) throws TammError, IOException {
    if (session.user == null) {
      String msg = Txt.get(session.lang, "missing_login");
      throw new TammError(msg);
    }
    
    TaskData advanceTask = gson.fromJson(reader, TaskData.class);
    TaskData taskData = application.tasks.readTask(session.client.id, advanceTask.lId);
    if (!isMyTask(session, taskData.owner)) {
      throw new TammError("Not your task.");
    }
    
    var rememberNextDueDate = taskData.nextDueDate;
    String next = nextDueDate(taskData);
    if (next.isEmpty()) {
      application.tasks.removeTask(taskData);
    } else {
      taskData.nextDueDate = next;
      application.tasks.writeTask(taskData, false);
    }
    
    // create history data record
    taskData.startDate = rememberNextDueDate;
    taskData.lastChanged = DateUtils.formatZ(null);
    application.history.writeTask(taskData, true);
    
    ServletUtils.sendResult(resultData, true, "", "", next, null);
    
  }
  
  private String nextDueDate(TaskData taskData) throws TammError {
    Interval interval = new Interval(taskData.interval);
    if (!interval.isValid()) {
      throw new TammError("Invalid interval.");
    }
    
    var result = interval.nextDate(taskData.nextDueDate);
    return (result == null) ? "" : result;
  }
  
  /**
   * Checks if the session user is allowed to process the task
   * with the given owner.
   * 
   * A main administrator is allowed to process all tasks.
   * If the current user is the owner then he is allowed to process it.
   * If the owner is a role and the current user is member then
   * he is allowed to process it.
   * 
   * @param session
   * @param owner
   * @return 
   */
  private boolean isMyTask(SessionData session, int owner) {
    if ((owner == session.user.id) || session.user.mainAdmin) {
      return true;
    }
    
    if ((session.myRoles != null) && (session.myRoles.roles != null)) {
      for (int roleId: session.myRoles.roles) {
        if (roleId == owner) {
          return true;
        }
      }
    } else {
      logger.info("User has no roles information: " + session.user.name);
    }
    
    return false;
  }
}
