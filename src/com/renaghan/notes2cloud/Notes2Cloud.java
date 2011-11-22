package com.renaghan.notes2cloud;

import com.apple.eawt.Application;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

  public void go() {
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

    Set<Event> notesEvents = new NotesDownload(minDate, maxDate).getEvents();
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

    // shutdown httpclient
    getUtils().close();
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
    macSetup();
    Notes2Cloud app = new Notes2Cloud();
    while (true) {
      try {
        app.go();
      } catch (Exception e) {
        LOG.error("Error running sync", e);
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
}
