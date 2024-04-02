/*
 * (c) 2024 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import de.mmth.tamm.data.TaskData;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Keep a report file with all saved tasks.
 * 
 * Fallback in case of loss of all database information.
 * 
 * @author matthias
 */
public class TaskReport {
  private static final org.apache.logging.log4j.Logger logger = TammLogger.prepareLogger(TaskReport.class);

  private final String uploadbase;
  private final Gson gson;
  
  /**
   * Constructor with file storage destination.
   * 
   * @param uploadbase 
   */
  public TaskReport(String uploadbase) {
    this.uploadbase = uploadbase;
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setLongSerializationPolicy( LongSerializationPolicy.STRING );
    gson = gsonBuilder.create();
  }
  
  /**
   * Add the given TaskData object into the report file.
   * 
   * The report file will be created if it does not exist yet.
   * The TaskData will be added as a JSON String.
   * 
   * @param task 
   */
  public void log(TaskData task) {
    File reportFile = new File(uploadbase, "tasks.txt");
    try {
      var taskJson = gson.toJson(task);
      Files.writeString(reportFile.toPath(), taskJson + "\r\n\r\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ex) {
      logger.warn("Error writing task report file", ex);
    }
  }
}
