/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mmth.tamm.data.AttachmentData;
import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.Logger;

/**
 * Document file upload and download.
 * 
 * @author matthias
 */
@WebServlet(name = "upload", urlPatterns = {"/upload/*"})
@MultipartConfig(
  fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
  maxFileSize = 1024 * 1024 * 10,      // 10 MB
  maxRequestSize = 1024 * 1024 * 100   // 100 MB
)
public class Upload extends HttpServlet {
  private static final Logger logger = TammLogger.prepareLogger(Upload.class);
  private final Gson gson = new GsonBuilder().create();
  private FileProcessor fileProcessor = null;
  
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    ApplicationData application = getApplication();
    ServletOutputStream out = response.getOutputStream();
    try {
      SessionData sd = ServletUtils.prepareSession(application, request);

      String requestUri = request.getRequestURI();
      String[] uriParts = requestUri.split("/");
      String guid = uriParts[3];
      String name = uriParts[4];

      if (!application.checkInit()) {
        logger.warn("Missing initialisation data, request aborted.");
        ServletUtils.gotoErrorPage(out);
        return;
      }

      if (!ServletUtils.checkClientId(request, sd, application)) {
        throw new TammError("Invalid client access.");
      }

      AttachmentData data = new AttachmentData();
      data.clientId = sd.client.id;
      data.guid = guid;
      data.fileName = name;
      File source = ServletUtils.prepareDestinationPath(application.rootPath, data);
      response.setHeader("Content-Disposition", "inline");
      Files.copy(source.toPath(), out);
    } catch(TammError te) {
      ServletUtils.sendResult(out, false, "", "", te.getMessage(), null);
    }
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    ApplicationData application = getApplication();
    SessionData sd = ServletUtils.prepareSession(application, request);
    long taskId = Long.parseLong(request.getParameter("taskid"));
    logger.info("Write attachments for task " + taskId);
    
    String message = "";
    List<String> result = new ArrayList<>();
    for (Part part : request.getParts()) {
      String fileName = part.getSubmittedFileName();
      if ((fileName == null) || fileName.isBlank()) {
        continue;
      }
      
      if (fileName.length() > 198) {
        fileName = fileName.substring(0, 150) + "..." + fileName.substring(fileName.length() - 45);
      }
      
      AttachmentData attachment = new AttachmentData();
      attachment.fileName = fileName;
      attachment.taskId = taskId;
      attachment.clientId = sd.client.id;
      attachment.guid = UUID.randomUUID().toString();
      logger.info(attachment.guid + " - " + attachment.fileName);
      
      result.add(attachment.guid);
      try {
        application.attachments.writeAttachment(attachment);
      } catch (TammError ex) {
        message = "Error writing file attachment.";
      }
              
      File dest = ServletUtils.prepareDestinationPath(application.rootPath, attachment);
      part.write(dest.getPath());
      logger.info("File written: " + dest.getPath());
    }
    
    ServletUtils.sendResult(response.getOutputStream(), message.isBlank(), "", "", message, result);
  }

  /**
   * Deletes an AttachmentData item from the database.
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException 
   */
  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    ApplicationData application = getApplication();
    SessionData sd = ServletUtils.prepareSession(application, request);
    String requestUri = request.getRequestURI();
    String[] uriParts = requestUri.split("/");
    String guid = uriParts[3];
      
    logger.info("Delete " + guid);
    AttachmentData att = new AttachmentData();
    att.guid = guid;
    att.clientId = sd.client.id;
    String message = "";
    try {
      application.attachments.removeAttachment(att);
      File dest = ServletUtils.prepareDestinationPath(application.rootPath, att);
      dest.delete();
    } catch (TammError ex) {
      message = "Error deleting attachment.";
      logger.warn(message, ex);
    }
    
    ServletUtils.sendResult(response.getOutputStream(), message.isBlank(), "", "", message, null);
  }
  
  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "TaMM upload.";
  }

  /**
   * Load application object and create
   * fileProcessor if needed.
   * 
   * The application object is created in
   * the system servlet. Since the order
   * of the init function calls are not
   * defined, this function should only
   * be called in the POST and GET parts.
   * 
   * @return application data object.
   */
  private ApplicationData getApplication() {
    var sc = getServletContext();
    var application = (ApplicationData) sc.getAttribute("application");
    if (fileProcessor == null) {
      fileProcessor = new FileProcessor(application);
    }
    
    return application;
  }
}
