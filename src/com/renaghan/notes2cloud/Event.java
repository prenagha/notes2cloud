package com.renaghan.notes2cloud;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

/**
 * A calendar event
 *
 * @author prenagha
 */
public class Event implements Serializable, Comparable<Event> {

  private final String uniqueId;
  private final String url;
  private final String name;
  private final String location;
  private final String owner;
  private final Date start;
  private final Date end;
  private final boolean allDay;

  public Event(String uniqueId, String name, String location, Date start, Date end, Date day, String owner) {
    this.url = null;
    this.uniqueId = uniqueId;
    this.name = name;
    this.location = location;
    this.owner = owner;

    if (end == null) {
      Calendar d = Calendar.getInstance();
      d.setTime(day);
      d.set(Calendar.HOUR_OF_DAY, 0);
      d.set(Calendar.MINUTE, 0);
      d.set(Calendar.SECOND, 0);
      d.set(Calendar.MILLISECOND, 0);
      this.start = d.getTime();
      d.add(Calendar.DATE, 1);
      this.end = d.getTime();
      this.allDay = true;
    } else {
      this.start = start;
      this.end = end;
      this.allDay = false;
    }
  }

  public Event(String url, String uniqueId, String name, String location, Date start, Date end, boolean allDay) {
    this.url = url;
    this.uniqueId = uniqueId;
    this.name = name;
    this.location = location;
    this.start = start;
    this.end = end;
    this.allDay = allDay;
    this.owner = null;
  }

  public String getUrl() {
    return url;
  }

  public String getOwner() {
    return owner;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public Date getStart() {
    return start;
  }

  public Date getEnd() {
    return end;
  }

  public boolean isAllDayEvent() {
    return allDay;
  }

  public String getUID() {
    String input = getName() + getLocation()
      + String.valueOf(start.getTime()) + String.valueOf(end.getTime())
      + String.valueOf(isAllDayEvent());
    try {
      byte[] bytesToBeEncrypted = input.getBytes("UTF-8");
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] theDigest = md.digest(bytesToBeEncrypted);
      // convert each byte to a hexadecimal digit
      Formatter formatter = new Formatter();
      for (byte b : theDigest) {
        formatter.format("%02x", b);
      }
      return formatter.toString().toUpperCase();
    } catch (Exception e) {
      throw new RuntimeException("Unable to get UID hash " + toString(), e);
    }
  }

  @Override
  public int compareTo(Event o) {
    int cmp = start.compareTo(o.start);
    if (cmp != 0) return cmp;
    cmp = end.compareTo(o.end);
    if (cmp != 0) return cmp;
    cmp = name.compareTo(o.name);
    if (cmp != 0) return cmp;
    return uniqueId.compareTo(o.uniqueId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Event))
      return false;
    Event event = (Event) o;
    if (!end.equals(event.end))
      return false;
    if (allDay != event.allDay)
      return false;
    if (location != null ? !location.equals(event.location) : event.location != null)
      return false;
    if (!name.equals(event.name))
      return false;
    return start.equals(event.start);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (location != null ? location.hashCode() : 0);
    result = 31 * result + start.hashCode();
    result = 31 * result + end.hashCode();
    result = 31 * result + Boolean.valueOf(allDay).hashCode();
    return result;
  }

  /**
   * @return string representation of object
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(200);
    sb.append("Event{");
    sb.append(name);
    sb.append(", ").append(start);
    if (!isAllDayEvent()) {
      sb.append(" to ").append(end);
    }
    if (location != null)
      sb.append(", @ ").append(location);
    sb.append("}");
    return sb.toString();
  }
}
