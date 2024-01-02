/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

package de.mmth.tamm;

import de.mmth.tamm.data.SessionData;
import de.mmth.tamm.utils.ServletUtils;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class System extends HttpServlet {
  
  private static final Logger logger = LogManager.getLogger(System.class);
  private final ApplicationData application = new ApplicationData();
  private final PostProcessor postProcessor = new PostProcessor(application);
  private final GetProcessor getProcessor = new GetProcessor(application);
  private final DeleteProcessor deleteProcessor = new DeleteProcessor(application);
  private final BackgroundWorker backgroundWorker = new BackgroundWorker(application);
  
  @Override
  public void init() {
    application.setSchema("tamm");
    var sc = this.getServletContext();
    sc.setAttribute("application", application);
    
    backgroundWorker.start();
  }
  
  @Override
  public void destroy() {
    logger.info("Stop background worker.");
    backgroundWorker.interrupt();
    
    if (application.rootPath != null) {
      File destination = new File(application.rootPath, "requestCache.lines");
      destination.delete();
      application.requests.save(destination.toPath());
    }
  }
  
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
    try (InputStream content = request.getInputStream()) {
      ServletOutputStream out = response.getOutputStream();
      try {
        SessionData sd = ServletUtils.prepareSession(request);

        String requestUri = request.getRequestURI();
        String[] uriParts = requestUri.split("/");
        String cmd = uriParts[3];

        if (!application.checkInit() && !cmd.equals("initdata")) {
          logger.warn("Missing initialisation data, request aborted.");
          ServletUtils.gotoErrorPage(out);
          return;
        }

        if (!ServletUtils.checkClientId(request, sd, application)) {
          throw new TammError("Invalid client access.");
        }

        String cmd4 = (uriParts.length > 4) ? uriParts[4] : "";
        if (!cmd4.isBlank()) {
          cmd4 = URLDecoder.decode(cmd4, StandardCharsets.UTF_8.toString());
        }
        
        if (cmd.equals("lockmail")) {
          response.setHeader("Content-Type", "text/html; charset=UTF-8");
        }
        
        getProcessor.process(sd, cmd, content, out, cmd4);
        out.flush();
      } catch(TammError te) {
        ServletUtils.sendResult(out, false, "", "", te.getMessage(), null);
      } catch(Throwable ex) {
        // dont leak internal exceptions to browser
        logger.warn("Unexpected error in get processing.", ex);
      }
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
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    if (application.tammUrl == null) {
      String url = request.getRequestURL().toString();
      int tammPos = url.indexOf("/TaMM/");
      if (tammPos > 0) {
        application.tammUrl = url.substring(0, tammPos + 6);
      }
    }

    try (InputStream content = request.getInputStream()) {
      SessionData sd = ServletUtils.prepareSession(request);
      ServletOutputStream out = response.getOutputStream();

      try {
        String requestUri = request.getRequestURI();
        String[] uriParts = requestUri.split("/");
        String cmd = uriParts[3];
        if (!application.checkInit() && !cmd.equals("initdata")) {
          logger.warn("Missing initialisation data, request aborted.");
          ServletUtils.gotoErrorPage(out);
          return;
        }

        if (!ServletUtils.checkClientId(request, sd, application)) {
          throw new TammError("Invalid client access.");
        }

        postProcessor.process(sd, cmd, content, out);
        out.flush();
      } catch(TammError te) {
        ServletUtils.sendResult(out, false, "", "", te.getMessage(), null);
      } catch(Throwable ex) {
        // dont leak internal exceptions to browser
        logger.warn("Unexpected error in post processing.", ex);
      }
    }
  }

  /**
   * Processes incoming delete requests.
   * 
   * @param request
   * @param response
   * @throws ServletException
   * @throws IOException 
   */
  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    try (InputStream content = request.getInputStream()) {
      ServletOutputStream out = response.getOutputStream();
      try {
        SessionData sd = ServletUtils.prepareSession(request);

        String requestUri = request.getRequestURI();
        String[] uriParts = requestUri.split("/");
        String cmd = uriParts[3];

        if (!application.checkInit() && !cmd.equals("initdata")) {
          logger.warn("Missing initialisation data, request aborted.");
          ServletUtils.gotoErrorPage(out);
          return;
        }

        if (!ServletUtils.checkClientId(request, sd, application)) {
          throw new TammError("Invalid client access.");
        }

        String cmd4 = (uriParts.length > 4) ? uriParts[4] : "";
        if (!cmd4.isBlank()) {
          cmd4 = URLDecoder.decode(cmd4, StandardCharsets.UTF_8.toString());
        }
        
        String cmd5 = (uriParts.length > 5) ? uriParts[5] : "";
        if (!cmd5.isBlank()) {
          cmd5 = URLDecoder.decode(cmd5, StandardCharsets.UTF_8.toString());
        }
        
        deleteProcessor.process(sd, cmd, content, out, cmd4, cmd5);
        out.flush();
      } catch(TammError te) {
        ServletUtils.sendResult(out, false, "", "", te.getMessage(), null);
      } catch(Throwable ex) {
        // dont leak internal exceptions to browser
        logger.warn("Unexpected error in delete processing.", ex);
      }
    }
  }
  
  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Task Management and Monitoring";
  }

}
