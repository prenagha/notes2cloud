package com.renaghan.notes2cloud;

import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.apple.eawt.Application;
import org.apache.log4j.Logger;

/**
 * Main class that runs the application
 *
 * @author prenagha
 */
public class Notes2Cloud {
  private static final Logger LOG = Logger.getLogger(Notes2Cloud.class);
  private static Utils utils = new Utils();

  public Notes2Cloud() {
  }

  public static void reloadUtils() {
    utils = new Utils();
  }

  public static Utils getUtils() {
    return utils;
  }

  public static void macSetup() {
    // set some mac-specific properties
    System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Notes2Cloud");

    // create an instance of the Mac Application class, so i can handle the
    // mac quit event with the Mac ApplicationAdapter
    Application macApplication = Application.getApplication();
    macApplication.addApplicationListener(new MacApp());
    macApplication.setEnabledPreferencesMenu(false);
    macApplication.setEnabledAboutMenu(false);
  }

  public static void main(String[] args) {
    LOG.info("Started");
    Calendar now = Calendar.getInstance();
    try {
      // manually trigger export with command line argument
      boolean export = args != null && args.length > 0 && args[0].equals("export");
      // otherwise if running 230-3 then do the export
      if (!export)
        export = now.get(Calendar.HOUR_OF_DAY) >= 10 && now.get(Calendar.HOUR_OF_DAY) <= 16;
      if (export)
        LOG.info("Run export = true");
      Notes2Cloud app = new Notes2Cloud();
      app.go(export);
    } catch (Exception e) {
      LOG.error("Error running sync", e);
      String errorDir = getUtils().getProperty("errorDir");
      if (errorDir != null && errorDir.length() > 2
        && now.get(Calendar.HOUR_OF_DAY) > 8 && now.get(Calendar.HOUR_OF_DAY) < 18) {
        File ed = new File(errorDir);
        if (ed.exists() && ed.canWrite()) {
          File ef = new File(ed, "Notes2Cloud-" + String.valueOf(System.currentTimeMillis()) + ".txt");
          try {
            PrintWriter pw = new PrintWriter(ef);
            e.printStackTrace(pw);
            pw.close();
          } catch (Exception x) {
            // do nothing
          }
        }
      }
    }
    LOG.info("Ended");
  }

  public static void mainMacApp(String[] args) {
    LOG.info("Started");
    macSetup();
    String errorDir = getUtils().getProperty("errorDir");
    int exportEvery = Integer.parseInt(getUtils().getProperty("export.runEveryIteration"));
    int exportIter = 0;
    Notes2Cloud app = new Notes2Cloud();
    while (true) {
      try {
        boolean export = exportIter == 0 || exportIter > exportEvery;
        app.go(export);
        if (export) {
          exportIter = 1;
        } else {
          exportIter++;
        }
      } catch (Exception e) {
        LOG.error("Error running sync", e);
        Calendar now = Calendar.getInstance();
        if (errorDir != null && errorDir.length() > 2
          && now.get(Calendar.HOUR_OF_DAY) > 8 && now.get(Calendar.HOUR_OF_DAY) < 18) {
          File ed = new File(errorDir);
          if (ed.exists() && ed.canWrite()) {
            File ef = new File(ed, "Notes2Cloud-" + String.valueOf(System.currentTimeMillis()) + ".txt");
            try {
              PrintWriter pw = new PrintWriter(ef);
              e.printStackTrace(pw);
              pw.close();
            } catch (Exception x) {
              // do nothing
            }
          }
        }
      }
      long mins = Long.parseLong(getUtils().getProperty("runEveryMinutes"));
      LOG.info("Waiting " + mins + " mins");
      try {
        Thread.sleep(mins * 60L * 1000L);
      } catch (InterruptedException e) {
        LOG.error("Interrupt on sleep, will end", e);
        break;
      }
    }
  }

  public void go(boolean export) {
    LOG.info("Go");
    reloadUtils();

    int daysBack = Integer.parseInt(utils.getProperty("daysBack"));
    Calendar minCal = Calendar.getInstance();
    minCal.add(Calendar.DATE, -1 * daysBack);
    minCal.set(Calendar.HOUR_OF_DAY, 0);
    minCal.set(Calendar.MINUTE, 0);
    minCal.set(Calendar.SECOND, 0);
    minCal.set(Calendar.MILLISECOND, 0);
    Date minDate = minCal.getTime();

    int daysForward = Integer.parseInt(utils.getProperty("daysForward"));
    Calendar maxCal = Calendar.getInstance();
    maxCal.add(Calendar.DATE, daysForward + 1);
    maxCal.set(Calendar.HOUR_OF_DAY, 0);
    maxCal.set(Calendar.MINUTE, 0);
    maxCal.set(Calendar.SECOND, 0);
    maxCal.set(Calendar.MILLISECOND, 0);
    Date maxDate = maxCal.getTime();

    Calendar todayCal = Calendar.getInstance();
    todayCal.set(Calendar.HOUR_OF_DAY, 0);
    todayCal.set(Calendar.MINUTE, 0);
    todayCal.set(Calendar.SECOND, 0);
    todayCal.set(Calendar.MILLISECOND, 0);
    Date today = todayCal.getTime();

    Set<Event> notesEvents = new NotesCalendarDownload(minDate, maxDate).getEvents();
    //Set<Event> notesEvents = new NotesFromFile(minDate, maxDate).getEvents();

    Set<Event> cloudEvents = new CloudDownload(minDate, maxDate).getEvents();

    Set<Event> deleteFromCloud = new HashSet<Event>(200);
    // find all cloud events today or later that don't have a match in notes, those need to be deleted from cloud
    for (Event cloudEvent : cloudEvents) {
      if (cloudEvent.getStart().after(today) && !notesEvents.contains(cloudEvent))
        deleteFromCloud.add(cloudEvent);
    }
    Set<Event> addToCloud = new HashSet<Event>(200);
    // find all notes events today or later that are not in cloud, those need to be added to cloud
    for (Event notesEvent : notesEvents) {
      if (notesEvent.getStart().after(today) && !cloudEvents.contains(notesEvent))
        addToCloud.add(notesEvent);
    }

    new CloudSynchronize(deleteFromCloud, addToCloud).go();

    if (export)
      new NotesMailExport().go();

    // shutdown httpclient
    getUtils().close();
  }
}
