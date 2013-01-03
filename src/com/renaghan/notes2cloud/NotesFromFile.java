package com.renaghan.notes2cloud;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;

/**
 * Reuse a previously downloaded notes response, useful for testing
 *
 * @author prenagha
 */
public class NotesFromFile extends NotesCalendarDownload {

  public NotesFromFile(Date minDate, Date maxDate) {
    super(minDate, maxDate);
  }

  @Override
  protected void login() {
    //do nothing
  }

  @Override
  protected void events() {
    // read the xml from local file and call parse
    File f = new File("notes-calendar-response.xml");
    try {
      Reader reader = new FileReader(f);
      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[8192];
      int read;
      while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
        builder.append(buffer, 0, read);
      }
      parse(builder.toString());
    } catch (Exception e) {
      throw new RuntimeException("Error getting notes calendar from file " + f, e);
    }
  }
}
