/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class BackgroundWorker extends Thread {
  private static final Logger logger = TammLogger.prepareLogger(BackgroundWorker.class);

  private final ApplicationData application;
  
  /**
   * Constructor with dependency injection of the application object.
   * @param application 
   */
  public BackgroundWorker(ApplicationData application) {
    this.application = application;
    this.setName("TaMM Background Worker");
  }
  
  /**
   * Processes background cleanup tasks.
   */
  @Override
  public void run() {
    logger.info("Start background worker.");
    while (this.isAlive() && !this.isInterrupted()) {
      if (application.requests != null) {
        application.requests.cleanup();
      }
      
      if (application.accessCache != null) {
        application.accessCache.cleanup();
      }
      
      if (application.mailCounter != null) {
        application.mailCounter.cleanup();
      }
      
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        logger.info("Background worker interrupted.");
        break;
      }
    }
    logger.info("Background worker stopped.");
  }
}
