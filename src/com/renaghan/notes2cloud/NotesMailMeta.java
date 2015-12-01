package com.renaghan.notes2cloud;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * meta data for a notes mail message
 *
 * @author prenagha
 */
public class NotesMailMeta {

  private static final Set<String> EXCLUDE = new HashSet<String>();

  static {
    EXCLUDE.add("210");
    EXCLUDE.add("11");
    EXCLUDE.add("212");
    EXCLUDE.add("196");
    EXCLUDE.add("205");
    EXCLUDE.add("194");
    EXCLUDE.add("200");
    EXCLUDE.add("202");
    EXCLUDE.add("198");
    EXCLUDE.add("123");
    EXCLUDE.add("157");
    EXCLUDE.add("195");
    EXCLUDE.add("197");
  }

  private final String id;
  private final String type;
  private final Calendar date;
  private final String subject;

  public NotesMailMeta(String id, String type, Calendar date, String subject) {
    this.id = id;
    this.type = type;
    this.date = date;
    this.subject = subject;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Calendar getDate() {
    return date;
  }

  public String getSubject() {
    return subject;
  }

  public String getFileName() {
    return String.valueOf(date.getTimeInMillis()) + "-" + id + "-notes";
  }

  public String getIMAPFileName() {
    return getFileName() + ":2,S";
  }

  public boolean isExport() {
    return type != null && !EXCLUDE.contains(type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    NotesMailMeta that = (NotesMailMeta) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Msg");
    sb.append("{");
    sb.append(new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(date.getTime()));
    sb.append(" type=").append(type);
    sb.append(", ").append(subject);
    sb.append(", ").append(id);
    sb.append('}');
    return sb.toString();
  }
}
