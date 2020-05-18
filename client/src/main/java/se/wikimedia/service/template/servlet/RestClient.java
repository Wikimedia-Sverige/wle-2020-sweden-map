package se.wikimedia.service.template.servlet;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kalle
 * @since 2017-09-28 19:11
 */
public abstract class RestClient implements Closeable {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private CloseableHttpClient httpClient;

  @Getter
  private String hostName;

  @Getter
  private int port;

  @Getter
  private String pathPrefix;

  private static Header applicationJson = new BasicHeader("Content-Type", "application/json");

  private static Pattern pathPrefixPattern = Pattern.compile("/?(api/)?(([0-9a-zA-Z\\-]+)(/[0-9a-zA-Z\\-]+)*)/?");

  protected RestClient(String hostName, int port, String pathPrefix) {
    this.hostName = hostName;
    this.port = port;

    if (pathPrefix != null) {
      Matcher matcher = pathPrefixPattern.matcher(pathPrefix);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Path prefix " + pathPrefix + " does not match " + pathPrefixPattern.pattern());
      }
      pathPrefix = matcher.group(2);
    }
    this.pathPrefix = pathPrefix;
  }

  private StringBuilder urlFactory() {
    StringBuilder sb = new StringBuilder(128)
        .append("http://").append(getHostName())
        .append(":").append(String.valueOf(getPort()))
        .append("/api/");
    if (getPathPrefix() != null) {
      sb.append(getPathPrefix());
      sb.append("/");
    }
    return sb;
  }

  private StringBuilder appendPath(StringBuilder url, String path) {
    if (path != null) {
      while (path.startsWith("/")) {
        path = path.substring(1);
      }
      url.append(path);
    }
    return url;
  }

  protected CloseableHttpResponse get(String path) throws Exception {
    HttpGet get = new HttpGet(appendPath(urlFactory(), path).toString());
    return execute(get);
  }

  protected CloseableHttpResponse put(String path, Object object) throws Exception {
    return put(path, objectMapper.writeValueAsString(object));
  }

  protected CloseableHttpResponse put(String path, String json) throws Exception {
    HttpPut put = new HttpPut(appendPath(urlFactory(), path).toString());
    put.addHeader(applicationJson);
    put.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
    return httpClient.execute(put);
  }

  protected CloseableHttpResponse put(String path) throws Exception {
    HttpPut put = new HttpPut(appendPath(urlFactory(), path).toString());
    return httpClient.execute(put);
  }

  protected CloseableHttpResponse post(String path, Object object) throws Exception {
    return post(path, objectMapper.writeValueAsString(object));
  }

  protected CloseableHttpResponse post(String path, String json) throws Exception {
    HttpPost post = new HttpPost(appendPath(urlFactory(), path).toString());
    post.addHeader(applicationJson);
    post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
    return execute(post);
  }

  protected CloseableHttpResponse post(String path) throws Exception {
    HttpPost post = new HttpPost(appendPath(urlFactory(), path).toString());
    return execute(post);
  }

  protected CloseableHttpResponse delete(String path) throws Exception {
    HttpDelete delete = new HttpDelete(appendPath(urlFactory(), path).toString());
    return execute(delete);
  }

  protected <R> R parse(CloseableHttpResponse response, Class<R> responseClass) throws IOException {
    try {
      if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
        return response.getEntity().getContentLength() == 0 ? null : objectMapper.readValue(response.getEntity().getContent(), responseClass);
      } else {
        throw new IOException("HTTP " + response.getStatusLine().getStatusCode());
      }
    } finally {
      response.close();
    }
  }

  protected <R extends Collection<T>, T> R parse(CloseableHttpResponse response, Class<R> collectionClass, Class<T> typeClass) throws IOException {
    try {
      if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(collectionClass, typeClass);
        return response.getEntity().getContentLength() == 0 ? null : objectMapper.readValue(response.getEntity().getContent(), type);
      } else {
        throw new IOException("HTTP " + response.getStatusLine().getStatusCode());
      }
    } finally {
      response.close();
    }
  }

  protected void consume(CloseableHttpResponse response) throws IOException {
    try {
      if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
        EntityUtils.consume(response.getEntity());
      } else {
        throw new IOException("HTTP " + response.getStatusLine().getStatusCode());
      }
    } finally {
      response.close();
    }
  }


  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException ioe) {
      log.warn("Caught exception while closing {}", httpClient, ioe);
    }
  }

  public static long defaultMaximumMillisecondsRetrying = TimeUnit.MINUTES.toMillis(5);
  public static long defaultMillisecondsBetweenRetries = TimeUnit.SECONDS.toMillis(10);

  @Getter
  @Setter
  private long maximumMillisecondsRetrying = defaultMaximumMillisecondsRetrying;

  @Getter
  @Setter
  private long millisecondsBetweenRetries = defaultMillisecondsBetweenRetries;

  private CloseableHttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
    return execute(request, maximumMillisecondsRetrying, millisecondsBetweenRetries);
  }

  private CloseableHttpResponse execute(HttpUriRequest request, long maximumMillisecondsRetrying, long millisecondsBetweenRetries) throws IOException, ClientProtocolException {
    int retry = 0;
    long timeout = System.currentTimeMillis() + maximumMillisecondsRetrying;
    while (true) {
      try {
        return httpClient.execute(request);
      } catch (java.net.ConnectException connectException) {
        if ("Connection refused".equals(connectException.getMessage())) {
          if (timeout > System.currentTimeMillis() + millisecondsBetweenRetries){
            try {
              retry++;
              if (log.isDebugEnabled()) {
                log.debug("Connection refused. Sleeping for {} milliseconds before retry #{}", millisecondsBetweenRetries, retry);
              }
              Thread.sleep(millisecondsBetweenRetries);
            } catch (InterruptedException ie) {
              log.warn("Caught interruption", ie);
            }
          } else {
            throw connectException;
          }
        } else {
          throw connectException;
        }
      }
    }
  }

}
