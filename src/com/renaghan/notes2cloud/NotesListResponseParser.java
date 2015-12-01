package com.renaghan.notes2cloud;

import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * parse notes mail export list response
 *
 * @author prenagha
 */
public class NotesListResponseParser {
  private static final Logger LOG = Logger.getLogger(NotesListResponseParser.class);

  public NotesListResponseParser() {
  }

  private XMLEventReader getXMLEventReader(Reader input)
    throws FactoryConfigurationError, XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    if (factory.isPropertySupported(XMLInputFactory.IS_VALIDATING)) {
      factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    }
    if (factory.isPropertySupported(XMLInputFactory.IS_COALESCING)) {
      factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    }
    factory.setXMLReporter(new XMLReporter() {
      @Override
      public void report(String message, String errorType,
                         Object relatedInformation, Location location)
        throws XMLStreamException {
        LOG.warn("XML error of type: " + errorType + " at line: " + location.getLineNumber() + " column: " + location.getColumnNumber() + ", message: " + message);
      }
    });
    return factory.createXMLEventReader(input);
  }

  public Collection<NotesMailMeta> convertXML(Reader input) throws IOException, XMLStreamException {
    XMLEventReader reader = getXMLEventReader(input);

    Collection<NotesMailMeta> messages = new ArrayList<NotesMailMeta>(700);
    while (reader.hasNext()) {
      XMLEvent next = reader.nextEvent();
      if (next.isStartElement()) {
        StartElement start = next.asStartElement();
        String startName = start.getName().getLocalPart();
        if ("viewentry".equals(startName)) {
          try {
            NotesMailMeta msg = loadViewEntry(start, reader);
            if (msg != null) {
              messages.add(msg);
              LOG.trace(msg);
              //if (messages.size() > 4) break;
            }
          } catch (ParseException e) {
            throw new RuntimeException("parse Error converting xml " + start, e);
          }
        }
      }
    }
    reader.close();
    return messages;
  }

  protected NotesMailMeta loadViewEntry(StartElement viewEntry, XMLEventReader reader) throws XMLStreamException, ParseException {
    Attribute unid = viewEntry.getAttributeByName(QName.valueOf("unid"));
    String id = unid.getValue();
    if (id == null)
      throw new RuntimeException("Item missing unid");
    id = id.trim();
    if (id == null)
      throw new RuntimeException("Item has empty unid");
    String type = null;
    Calendar date = null;
    String subject = null;
    String name = null;
    try {
      while (reader.hasNext()) {
        XMLEvent next = reader.nextEvent();
        if (next.isStartElement()) {
          StartElement start = next.asStartElement();
          String startName = start.getName().getLocalPart();
          if ("entrydata".equals(startName)) {
            name = start.getAttributeByName(QName.valueOf("name")).getValue();
            if (type == null && ("$86".equals(name) || "$149".equals(name))) { // type
              type = readSingleSubElementValue(reader, start);
              if (type == null)
                throw new RuntimeException("Type is empty");
            } else if (date == null && ("$68".equals(name) || "$134".equals(name) || "$144".equals(name))) { // date
              String dateStr = readSingleSubElementValue(reader, start);
              if (dateStr == null)
                throw new RuntimeException("Date is null");
              date = convertTimeFormat(dateStr);
            } else if (subject == null && ("$74".equals(name) || "$147".equals(name))) { // subject
              subject = readSingleSubElementValue(reader, start);
            }
          }
        } else if (next.isEndElement()) {
          if ("viewentry".equals(next.asEndElement().getName().getLocalPart())) {
            return new NotesMailMeta(id, type, date, subject);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error parsing " + id + " " + name, e);
    }
    throw new RuntimeException("Can't load view entry " + id);
  }

  protected String readSingleSubElementValue(XMLEventReader reader, StartElement start) throws XMLStreamException {
    String value = null;
    while (reader.hasNext()) {
      XMLEvent next = reader.nextEvent();
      if (value == null && next.isCharacters()) {
        value = next.asCharacters().getData();
        value = value == null ? value : value.trim();
        value = value == null || value.isEmpty() ? null : value;
      } else if (next.isEndElement() &&
        start.getName().getLocalPart().equals(next.asEndElement().getName().getLocalPart())) {
        break;
      }
    }
    return value;
  }

  private Calendar convertTimeFormat(String dateStr) throws ParseException {
    String year, month, day, hour, minute, second, timezone1, timezone;

    if (dateStr == null)
      return null;

    if (dateStr.length() == 8) {
      year = dateStr.substring(0, 4);
      month = dateStr.substring(4, 6);
      day = dateStr.substring(6, 8);
      hour = "00";
      minute = "00";
      second = "00";
      String str = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date sd = sdf.parse(str);
      Calendar c = Calendar.getInstance();
      c.setTime(sd);
      return c;
    }

    if (dateStr.length() < 18)
      throw new IllegalArgumentException("Notes time too short " + dateStr);

    // 20100405T170000,00-04
    // 20100412T115157,95-04
    year = dateStr.substring(0, 4);
    month = dateStr.substring(4, 6);
    day = dateStr.substring(6, 8);
    hour = dateStr.substring(9, 11);
    minute = dateStr.substring(11, 13);
    second = dateStr.substring(13, 15);
    timezone1 = dateStr.substring(18, 21);
    //timezone2 = lotusnotesDateTimeFormat.substring(16, 18);
    timezone = timezone1 + "00";

    String str = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second + timezone;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    Date sd = sdf.parse(str);
    Calendar c = Calendar.getInstance();
    c.setTime(sd);
    return c;
  }
}
