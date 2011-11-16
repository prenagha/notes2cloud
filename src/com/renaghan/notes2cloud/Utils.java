package com.renaghan.notes2cloud;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Notes2Cloud utilities
 *
 * @author prenagha
 */
public class Utils {

  private final Properties props;
  private final DefaultHttpClient http;

  public Utils() {
    try {
      Properties dflt = new Properties();
      InputStream is = ClassLoader.getSystemResourceAsStream("notes2cloud.properties");
      if (is == null)
        throw new RuntimeException("Can't find notes2cloud.properties on classpath");
      dflt.load(is);
      is.close();

      String userFileName = System.getProperty("user.home") + File.separator + "notes2cloud.properties";
      File userFile = new File(userFileName);
      if (!userFile.exists())
        throw new RuntimeException("Cannot find user property file " + userFileName);
      if (!userFile.canRead())
        throw new RuntimeException("User property file not readable " + userFileName);
      FileInputStream fis = new FileInputStream(userFile);
      props = new Properties(dflt);
      props.load(fis);
      fis.close();

    } catch (Exception e) {
      throw new RuntimeException("Error loading properties", e);
    }

    try {
      HttpParams params = new BasicHttpParams();
      params.setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
      params.setParameter(CoreProtocolPNames.USER_AGENT, getProperty("userAgent"));
      http = new DefaultHttpClient(new SingleClientConnManager());

      if ("false".equals(getProperty("sslValidation"))) {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
          public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          }

          public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          }

          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        }}, null);
        SSLSocketFactory factory = new SSLSocketFactory(sslContext, new AllowAllHostnameVerifier());
        Scheme https = new Scheme("https", 443, factory);
        SchemeRegistry schemeRegistry = http.getConnectionManager().getSchemeRegistry();
        schemeRegistry.register(https);
      }

      URI uri = new URI(getProperty("cloud.baseUrl"));
      http.getCredentialsProvider().setCredentials(
        new AuthScope(uri.getHost(), uri.getPort()),
        new UsernamePasswordCredentials(getProperty("cloud.userId"), getProperty("cloud.password")));

    } catch (Exception e) {
      throw new RuntimeException("Error creating httpclient", e);
    }
  }

  public String getProperty(String key) {
    String v = System.getProperty(key);
    if (v != null)
      v =  v.trim();
    if (v == null || v.length() == 0)
      v = props.getProperty(key);
    if (v != null)
      v = v.trim();
    if (v == null || v.length() == 0)
      throw new IllegalArgumentException("Property not found " + key);
    return v;
  }

  public HttpClient getHttpClient() {
    return http;
  }

  public void close() {
    http.getConnectionManager().shutdown();
  }
}
