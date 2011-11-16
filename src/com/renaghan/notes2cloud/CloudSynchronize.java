package com.renaghan.notes2cloud;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

/**
 * TODO
 *
 * @author prenagha
 */
public class CloudSynchronize {
  private static final Logger LOG = Logger.getLogger(CloudSynchronize.class);

  private final Utils utils = Notes2Cloud.getUtils();
  private final Set<Event> deleteFromCloud;
  private final Set<Event> addToCloud;
  private final SimpleDateFormat utc = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
  private final SimpleDateFormat shortFormat = new SimpleDateFormat("yyyyMMdd");

  public CloudSynchronize(Set<Event> deleteFromCloud, Set<Event> addToCloud) {
    this.deleteFromCloud = deleteFromCloud;
    this.addToCloud = addToCloud;
    this.utc.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public void go() {
    delete();
    add();
  }

  private void delete() {
    if (deleteFromCloud.isEmpty())
      LOG.info("Nothing to delete from cloud");
    String calBaseUrl = utils.getProperty("cloud.baseUrl");
    for (Event event : deleteFromCloud) {
      try {
        String url = calBaseUrl + event.getUrl();
        HttpDelete delete = new HttpDelete(url);
        HttpClient http = utils.getHttpClient();
        HttpResponse response = http.execute(delete);
        EntityUtils.consume(response.getEntity());
        LOG.info("Cloud Delete: " + response.getStatusLine().getStatusCode() + " " + event);
      } catch (Exception e) {
        LOG.error("Error deleting event " + event, e);
      }
    }
  }

  private void add() {
    if (addToCloud.isEmpty())
      LOG.info("Nothing to add to cloud");
    String calUrl = utils.getProperty("cloud.baseUrl") + utils.getProperty("cloud.calendarUrl");
    for (Event event : addToCloud) {
      try {
        String uid = event.getUID();
        String url = calUrl + uid + ".ics";
        String req = "BEGIN:VCALENDAR";
        req += "\nVERSION:2.0\nPRODID:-//Notes2Cloud//CalDAV Client//EN\n";
        req += "CALSCALE:GREGORIAN";
        req += "\nBEGIN:VEVENT";
        req += "\nUID:" + uid;
        req += "\nSUMMARY:" + escape(event.getName());
        req += "\nDTSTART" + toString(event, event.getStart());
        req += "\nDTEND" + toString(event, event.getEnd());
        if (event.getLocation() != null)
          req += "\nLOCATION:" + escape(event.getLocation());
        if (event.getOwner() != null)
          req += "\nDESCRIPTION:" + escape(event.getOwner());
        req += "\nEND:VEVENT\nEND:VCALENDAR\n";
        HttpPut put = new HttpPut(url);
        put.setHeader("Content-Type", "text/calendar");
        put.setEntity(new StringEntity(req));
        HttpClient http = utils.getHttpClient();
        HttpResponse response = http.execute(put);
        String output = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
        LOG.info("Cloud Add: " + response.getStatusLine().getStatusCode() + " " + event);
        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() > 299) {
          LOG.error("A: " + event + "\n" + url + "\n" + req);
          LOG.error(response.getStatusLine());
          for (Header hdr : response.getAllHeaders()) {
            LOG.error(hdr);
          }
          LOG.error(output);
          LOG.error("Error adding event " + event);
        }
      } catch (Exception e) {
        LOG.error("Error adding event " + event, e);
      }
    }
  }

  private String toString(Event event, Date dt) {
    if (event.isAllDayEvent()) {
      return ";VALUE=DATE:" + shortFormat.format(dt);
    }
    return toString(dt);
  }

  private String toString(Date dt) {
    return ":" + utc.format(dt) + "Z";
  }

  private String escape(String input) {
    return input == null ? null : input.replace(",","\\,").replace(";","\\;");
  }

}
