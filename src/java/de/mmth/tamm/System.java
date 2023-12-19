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
import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
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
  
  @Override
  public void init() {
    application.setSchema("tamm");
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
    try (InputStream content = request.getInputStream()) {
      HttpSession session = request.getSession();
      SessionData sd = (SessionData) session.getAttribute("TAMM");
      if (sd == null) {
        sd = new SessionData();
        session.setAttribute("TAMM", sd);
        sd.clientIp = ServletUtils.getClientIp(request);
        logger.info("New session from address " + sd.clientIp);
      }
      
      try {
        String requestUri = request.getRequestURI();
        ServletOutputStream out = response.getOutputStream();
        postProcessor.process(sd, requestUri, content, out);
        out.flush();
      } catch(Throwable ex) {
        // dont leak internal exceptions to browser
        logger.warn("Unexpected error in post processing.", ex);
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
