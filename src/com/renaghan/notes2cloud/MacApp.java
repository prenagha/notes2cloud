package com.renaghan.notes2cloud;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import org.apache.log4j.Logger;

/**
 * To get clean shutdown on mac
 *
 * @author prenagha
 */
public class MacApp extends ApplicationAdapter {
  private static final Logger LOG = Logger.getLogger(MacApp.class);

  public MacApp() {
  }

  public void handleQuit(ApplicationEvent e) {
    LOG.info("Quit");
    System.exit(0);
  }
}
