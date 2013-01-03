package com.renaghan.notes2cloud;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Download entries from notes calendar
 *
 * @author prenagha
 */
public class NotesCalendarDownload {
  private static final SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyyMMdd");

  private final Utils utils = Notes2Cloud.getUtils();
  private final Set<Event> events = new LinkedHashSet<Event>(200);
  private final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private final Date minDate;
  private final Date maxDate;

  public NotesCalendarDownload(Date minDate, Date maxDate) {
    this.minDate = minDate;
    this.maxDate = maxDate;
    login();
    events();
  }

  public Set<Event> getEvents() {
    return events;
  }

  protected void login() {
    new NotesLogin().login();
  }

  protected void events() {
    String minDateStr = YYYYMMDD.format(minDate);
    String calUrl = utils.getProperty("notes.url") + utils.getProperty("notes.calendarUrl")
      + "&StartKey=" + minDateStr + "T000001,00Z";
    try {
      HttpGet calendar = new HttpGet(calUrl);
      HttpResponse response = utils.getHttpClient().execute(calendar);
      String xml = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
      if ("true".equals(Notes2Cloud.getUtils().getProperty("debug"))) {
        FileWriter fw = new FileWriter("notes-calendar-response.xml");
        fw.write(xml);
        fw.close();
      }
      parse(xml);
    } catch (Exception e) {
      throw new RuntimeException("Error getting notes calendar " + calUrl, e);
    }
  }

  protected void parse(String xml) {
    String startTimeFieldName = utils.getProperty("notes.startTimeFieldName");
    String endTimeFieldName = utils.getProperty("notes.endTimeFieldName");
    String dayTimeFieldName = utils.getProperty("notes.dayTimeFieldName");
    String subjectFieldName = utils.getProperty("notes.subjectFieldName");
    String excludeWithPrefix = utils.getProperty("excludeWithPrefix");
    String excludeContains = utils.getProperty("excludeContains");

    String current = "";
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));

      NodeList viewentries = doc.getElementsByTagName("viewentry");
      for (int i = 0; i < viewentries.getLength(); i++) {
        Element viewentry = (Element) viewentries.item(i);
        String uniqueId = viewentry.getAttribute("unid");
        current = uniqueId;
        String name = null;
        Date start = null;
        Date end = null;
        Date day = null;
        String location = null;
        String owner = null;
        NodeList entrydatas = viewentry.getElementsByTagName("entrydata");
        for (int j = 0; j < entrydatas.getLength(); j++) {
          Element entrydata = (Element) entrydatas.item(j);
          String fieldName = entrydata.getAttribute("name");
          if (startTimeFieldName.equals(fieldName)) {
            start = getDateTime(entrydata);
          } else if (endTimeFieldName.equals(fieldName)) {
            end = getDateTime(entrydata);
          } else if (dayTimeFieldName.equals(fieldName)) {
            day = getDateTime(entrydata);
          } else if (subjectFieldName.equals(fieldName)) {
            NodeList textlists = entrydata.getElementsByTagName("textlist");
            if (textlists.getLength() >= 1) {
              NodeList texts = textlists.item(0).getChildNodes();
              name = texts.item(0).getTextContent();
              if (texts.getLength() >= 2) {
                String o = texts.item(texts.getLength() - 1).getTextContent();
                String parts[] = o.split(" ");
                if (parts.length == 3) {
                  owner = parts[0] + " " + parts[2];
                } else {
                  owner = o;
                }
              }
              if (texts.getLength() >= 3)
                location = texts.item(1).getTextContent();
            } else {
              name = entrydata.getElementsByTagName("text").item(0).getTextContent();
            }
          }
        }

        Date when = start == null ? day : start;
        if (uniqueId != null
          && name != null
          && when != null
          && !name.startsWith(excludeWithPrefix)
          && !name.contains(excludeContains)
          && !when.after(maxDate)
          && !when.before(minDate)) {
          events.add(new Event(uniqueId, name, location, start, end, day, owner));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error parsing notes calendar xml -- " + current, e);
    }
  }

  private Date getDateTime(Element entrydata) throws Exception {
    Element datetime;
    NodeList dateTimes = entrydata.getElementsByTagName("datetimelist");
    if (dateTimes.getLength() > 0) {
      datetime = (Element) dateTimes.item(0).getChildNodes().item(0);
    } else {
      datetime = (Element) entrydata.getElementsByTagName("datetime").item(0);
    }
    return datetime == null ? null : toDate(datetime.getTextContent());
  }

  private Date toDate(String dt) throws Exception {
    if (dt == null)
      return null;

    if (dt.length() < 18)
      throw new IllegalArgumentException("Notes time too short " + dt);

    String year, month, day, hour, minute, second, timezone1, timezone2, timezone;
    year = dt.substring(0, 4);
    month = dt.substring(4, 6);
    day = dt.substring(6, 8);
    hour = dt.substring(9, 11);
    minute = dt.substring(11, 13);
    second = dt.substring(13, 15);
    timezone1 = dt.substring(18, 21);
    timezone2 = dt.substring(16, 18);
    timezone = timezone1 + timezone2;

    String fullDt = year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":" + second + timezone;

    return parser.parse(fullDt);
  }


}
