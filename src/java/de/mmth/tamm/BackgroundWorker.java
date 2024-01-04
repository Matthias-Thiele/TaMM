/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author matthias
 */
public class BackgroundWorker extends Thread {
  private static final Logger logger = LogManager.getLogger(BackgroundWorker.class);

  private final ApplicationData application;
  
  public BackgroundWorker(ApplicationData application) {
    this.application = application;
  }
  
  public void run() {
    logger.info("Start background worker.");
    while (this.isAlive() && !this.isInterrupted()) {
      application.requests.cleanup();
      application.accessCache.cleanup();
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
