package com.renaghan.notes2cloud;

import org.apache.http.client.methods.HttpPost;

/**
 * CalDav REPORT HTTP method
 *
 * @author prenagha
 */
public class HttpCalDavReport extends HttpPost {

  public final static String METHOD_NAME = "REPORT";

  /**
   * @throws IllegalArgumentException if the uri is invalid.
   */
  public HttpCalDavReport(String uri) {
    super(uri);
  }

  
  @Override
  public String getMethod() {
    return METHOD_NAME;
  }
}
