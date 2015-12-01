package com.renaghan.notes2cloud;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * export messages from lotus notes
 *
 * @author prenagha
 */
public class NotesMailExport {
  private static final Logger LOG = Logger.getLogger(NotesMailExport.class);
  private final Utils utils = Notes2Cloud.getUtils();
  private final static int READ_SIZE = 500;
  private final Collection<NotesMailMeta> messages = new LinkedHashSet<NotesMailMeta>(2000);

  public NotesMailExport() {
  }

  public void go() {
    loadList();
    exportList();
  }

  private void exportList() {
    Calendar breather = Calendar.getInstance();
    breather.add(Calendar.MINUTE, -30);
    int total = messages.size();
    int item = 0;
    for (NotesMailMeta msg : messages) {
      item++;
      // we don't archive calendar to email
      if (!msg.isExport()) {
        //LOG.debug("Skip: " + msg);
        continue;
      }
      // give a few hours to clear new stuff from inbox
      if (msg.getDate().after(breather))
        continue;
      // export directory is based on year of message
      String year = String.valueOf(msg.getDate().get(Calendar.YEAR));
      File outputDir = new File(utils.getProperty("export.outputDir") + "/" + year + "/cur");
      if (!outputDir.exists())
        throw new RuntimeException("Output dir does not exist " + outputDir + " " + msg);
      if (!outputDir.canRead() || !outputDir.canWrite())
        throw new RuntimeException("Output dir not accessible " + outputDir + " " + msg);
      // if we already have exported this message then skip
      File msgFile = new File(outputDir, msg.getIMAPFileName());
      if (msgFile.exists())
        continue;
      if (contains(outputDir, msg.getFileName()))
        continue;
      exportMessage(item, total, msg, outputDir, msgFile);
    }
  }

  private boolean contains (File dir, final String startsWith) {
    if (dir == null || startsWith == null || startsWith.length() < 10)
      return false;
    File[] files = dir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith(startsWith);
      }
    });
    return files != null && files.length > 0;
  }

  private void exportMessage(int item, int total, NotesMailMeta msg, File outputDir, File msgFile) {
    LOG.info(item + "/" + total + " Export " + msg);
    String msgUrl = utils.getProperty("notes.url")
      + utils.getProperty("export.msgUrl")
      + msg.getId()
      + utils.getProperty("export.msgFields");

    try {
      HttpGet contents = new HttpGet(msgUrl);
      HttpResponse response = utils.getHttpClient().execute(contents);
      if (response.getStatusLine().getStatusCode() == 200) {
        //export worked
        File tmp = new File(outputDir, ".msg.tmp");
        if (tmp.exists())
          tmp.delete();
        InputStream is = response.getEntity().getContent();
        FileOutputStream os = new FileOutputStream(msgFile);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        byte[] buffer = new byte[8192];
        int len;
        while ((len = is.read(buffer)) != -1) {
          bos.write(buffer, 0, len);
        }
        is.close();
        bos.close();
        tmp.renameTo(msgFile);
        msgFile.setLastModified(msg.getDate().getTimeInMillis());
      } else {
        //export failed
        throw new RuntimeException("Notes export mail failed " + response.getStatusLine().getStatusCode() + " " + msg);
      }
      LOG.info("Fix mail export " + msgFile.getCanonicalPath());
      try {
        Runtime.getRuntime().exec("/usr/bin/perl /Users/prenagha/Dev/notes2cloud/fix-mail-export.pl " + msgFile.getCanonicalPath());
      } catch (Exception e) {
        LOG.error("Error running perl fixDate script on " + msgFile.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error in mail export " + msgUrl + " -- " + msg, e);
    }

  }

  private void loadList() {
    int total = 0;
    int start = 1;
    while (true) {
      LOG.info("List reading from " + start);
      int read = listStarting(start);
      total = total + read;
      LOG.info("List read count " + read);
      if (read < READ_SIZE)
        break;
      start = total;
    }
    LOG.info("Total read " + total);
  }

  private int listStarting(int start) {
    String listUrl = utils.getProperty("notes.url")
      + utils.getProperty("export.listUrl")
      + "&Start=" + start
      + "&Count=" + READ_SIZE
      + utils.getProperty("export.listFields");
    try {
      HttpGet list = new HttpGet(listUrl);
      HttpResponse response = utils.getHttpClient().execute(list);
      String output = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
      if ("true".equals(Notes2Cloud.getUtils().getProperty("debug"))) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          FileWriter fw = new FileWriter("notes-export-list-response.xml");
          fw.write(output);
          fw.close();
        }
      }
      if (response.getStatusLine().getStatusCode() == 200) {
        //list worked
      } else {
        //list failed
        throw new RuntimeException("Notes export mail list failed " + response.getStatusLine().getStatusCode());
      }
      Reader r = new StringReader(output);
      Collection<NotesMailMeta> ms = new NotesListResponseParser().convertXML(r);
      r.close();
      if (ms != null && !ms.isEmpty())
        messages.addAll(ms);
      return ms == null ? 0 : ms.size();
    } catch (Exception e) {
      throw new RuntimeException("Error in mail export list " + listUrl, e);
    }
  }

  public static void main(String[] args) {
    try {
      LOG.info("Start");
      NotesMailExport exp = new NotesMailExport();
      new NotesLogin().login();
      exp.loadList();
      exp.exportList();
      exp.utils.close();
      LOG.info("End");
    } catch (Throwable t) {
      LOG.error("Error in mail export", t);
    }
  }
}
