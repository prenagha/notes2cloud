package com.renaghan.notes2cloud;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * login to lotus notes server, get session cookies into httpclient
 *
 * @author prenagha
 */
public class NotesLogin {
  private static final Logger LOG = Logger.getLogger(NotesLogin.class);

  private final Utils utils = Notes2Cloud.getUtils();

  void login() {
    String loginUrl = utils.getProperty("notes.url") + utils.getProperty("notes.loginUrl");
    try {
      HttpPost login = new HttpPost(loginUrl);
      List<NameValuePair> data = new ArrayList<NameValuePair>() {{
        add(new BasicNameValuePair("username", utils.getProperty("notes.userId")));
        add(new BasicNameValuePair("password", utils.getProperty("notes.password")));
      }};
      login.setEntity(new UrlEncodedFormEntity(data));
      LOG.info("Login attempt");
      HttpResponse response = utils.getHttpClient().execute(login);
      String output = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
      if ("true".equals(Notes2Cloud.getUtils().getProperty("debug"))) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          FileWriter fw = new FileWriter("notes-login-response.html");
          fw.write(output);
          fw.close();
        }
      }
      if (response.getStatusLine().getStatusCode() == 302) {
        //login worked
      } else {
        //login failed
        throw new RuntimeException("Login to notes failed " + response.getStatusLine().getStatusCode());
      }
    } catch (Exception e) {
      throw new RuntimeException("Error logging in to notes " + loginUrl, e);
    }
    LOG.info("Login success");
  }
}
