package com.renaghan.notes2cloud;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
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
import java.util.*;

/**
 * Download entries from cloud
 *
 * @author prenagha
 */
public class CloudDownload {
  private final SimpleDateFormat utc = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
  private final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  private final Utils utils = Notes2Cloud.getUtils();
  private final Set<Event> events = new LinkedHashSet<Event>(200);
  private final Date minDate;
  private final Date maxDate;

  public CloudDownload(Date minDate, Date maxDate) {
    utc.setTimeZone(TimeZone.getTimeZone("UTC"));
    this.minDate = minDate;
    this.maxDate = maxDate;
    events();
  }

  public Set<Event> getEvents() {
    return events;
  }

  private void events() {

    String calUrl = utils.getProperty("cloud.baseUrl") + utils.getProperty("cloud.calendarUrl");
    try {
      HttpCalDavReport calendar = new HttpCalDavReport(calUrl);

      String req = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
        "   <C:calendar-query xmlns:D=\"DAV:\"\n" +
        "                 xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
        "     <D:prop>\n" +
        "       <C:calendar-data>\n" +
        "         <C:comp name=\"VCALENDAR\">\n" +
        "           <C:comp name=\"VEVENT\">\n" +
        "             <C:prop name=\"SUMMARY\"/>\n" +
        "             <C:prop name=\"UID\"/>\n" +
        "             <C:prop name=\"DTSTART\"/>\n" +
        "             <C:prop name=\"DTEND\"/>\n" +
        "             <C:prop name=\"LOCATION\"/>\n" +
        "             <C:prop name=\"DESCRIPTION\"/>\n" +
        "           </C:comp>\n" +
        "           <C:comp name=\"VTIMEZONE\"/>\n" +
        "         </C:comp>\n" +
        
        "       </C:calendar-data>\n" +
        "     </D:prop>\n" +
        "     <C:filter>\n" +
        "       <C:comp-filter name=\"VCALENDAR\">\n" +
        "         <C:comp-filter name=\"VEVENT\">\n" +
        "           <C:time-range start=\"" + utc.format(minDate) + "\"\n" +
        "                         end=\"" + utc.format(maxDate) + "\"/>\n" +
        "         </C:comp-filter>\n" +
        "       </C:comp-filter>\n" +
        "     </C:filter>\n" +
        "   </C:calendar-query>";

      calendar.setEntity(new StringEntity(req));
      HttpResponse response = utils.getHttpClient().execute(calendar);
      String xml = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
      if ("true".equals(Notes2Cloud.getUtils().getProperty("debug"))) {
        FileWriter fw = new FileWriter("cloud-calendar-response.xml");
        fw.write(xml);
        fw.close();
      }
      parse(xml);
    } catch (Exception e) {
      throw new RuntimeException("Error getting cloud calendar " + calUrl, e);
    }
  }

  private void parse(String xml) {
    String item = "";
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));

      NodeList responses = doc.getElementsByTagName("response");
      for (int r = 0; r < responses.getLength(); r++) {
        Element response = (Element) responses.item(r);
        Element hrefTag = (Element) response.getElementsByTagName("href").item(0);
        String href = hrefTag.getTextContent();
        Element calendardata = (Element) response.getElementsByTagName("calendar-data").item(0);
        item = calendardata.getTextContent();
        String uniqueId = null;
        String name = null;
        Date start = null;
        Date end = null;
        String location = null;
        boolean allDay = false;
        Map<String, String> lines = parseLines(item);
        for (Map.Entry<String, String> entry : lines.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if ("SUMMARY".equals(key)) {
            name = unescape(value);
          } else if ("UID".equals(key)) {
            uniqueId = value;
          } else if ("DTSTART".equals(key)) {
            allDay = value.startsWith("VALUE=DATE:");
            start = toDate(value);
          } else if ("DTEND".equals(key)) {
            end = toDate(value);
          } else if ("LOCATION".equals(key)) {
            location = unescape(value);
          }
        }
        if (uniqueId != null
          && name != null
          && start != null
          && end != null
          && !start.after(maxDate)
          && !start.before(minDate)) {
          events.add(new Event(href, uniqueId, name, location, start, end, allDay));
        }
      }

    } catch (Exception e) {
      throw new RuntimeException("Error parsing cloud calendar xml -- " + item, e);
    }
  }

  private Map<String, String> parseLines(String element) {
    LinkedHashMap<String, String> lines = new LinkedHashMap<String, String>();

    StringTokenizer tokenizer = new StringTokenizer(element, "\n", false);
    String lastKey = "";
    while (tokenizer.hasMoreTokens()) {
      String line = tokenizer.nextToken();
      if (line.startsWith(" ")) {
        lines.put(lastKey, lines.get(lastKey) + line.substring(1));
      }
      int delim = line.indexOf(":");
      int semi = line.indexOf(";");
      if (semi > 0 && semi < delim)
        delim = semi;
      if (delim > 0) {
        lastKey = line.substring(0, delim).trim();
        lines.put(lastKey, line.substring(delim + 1));
      }
    }
    return lines;
  }

  private Date toDate(String dt) throws Exception {
    if (dt == null)
      return null;

    String year, month, day, hour, minute, second;
    TimeZone tz = TimeZone.getDefault();
    if (dt.startsWith("TZID=")) {
      // TZID=America/New_York:20111110T150000
      int eq = dt.indexOf("=");
      int colon = dt.indexOf(":");
      String timezone = dt.substring(eq + 1, colon);
      tz = TimeZone.getTimeZone(timezone);
      int st = colon + 1;
      year = dt.substring(st, st + 4);
      month = dt.substring(st + 4, st + 6);
      day = dt.substring(st + 6, st + 8);
      hour = dt.substring(st + 9, st + 11);
      minute = dt.substring(st + 11, st + 13);
      second = dt.substring(st + 13, st + 15);

    } else if (dt.startsWith("VALUE=DATE:")) {
      // DTSTART;VALUE=DATE:20111110
      year = dt.substring(11, 15);
      month = dt.substring(15, 17);
      day = dt.substring(17, 19);
      hour = "00";
      minute = "00";
      second = "00";

    } else if (dt.endsWith("Z")) {
      tz = TimeZone.getTimeZone("UTC");
      int st = 0;
      year = dt.substring(st, st + 4);
      month = dt.substring(st + 4, st + 6);
      day = dt.substring(st + 6, st + 8);
      hour = dt.substring(st + 9, st + 11);
      minute = dt.substring(st + 11, st + 13);
      second = dt.substring(st + 13, st + 15);

    } else {
      throw new IllegalArgumentException("Invalid cloud date " + dt);
    }

    parser.setTimeZone(tz);
    String fullDt = year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":" + second;
    return parser.parse(fullDt);
  }

  private String unescape(String input) {
    return input == null ? null : input.replace("\\;",";").replace("\\,",",");
  }
}
