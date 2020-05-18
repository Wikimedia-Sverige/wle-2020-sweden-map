package se.wikimedia.service.template.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kalle
 * @since 2017-03-27
 */
public class RestClient {

  @Inject
  private ObjectMapper objectMapper;

  private CloseableHttpClient client;

  public void open() throws IOException {
    client = HttpClientBuilder.create().build();
  }

  public void close() throws IOException {
    client.close();
  }

  protected <ResponseClass> ResponseClass post(Object request, String url, Class<ResponseClass> responseClass) throws IOException {
    HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
    return processResponse(client.execute(post), responseClass);
  }

  protected <ResponseClass> ResponseClass put(Object request, String url, Class<ResponseClass> responseClass) throws IOException {
    HttpPut put = new HttpPut(url);
    put.setEntity(new StringEntity(objectMapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
    return processResponse(client.execute(put), responseClass);
  }

  protected <ResponseClass> ResponseClass delete(String url, Class<ResponseClass> responseClass) throws IOException {
    return processResponse(client.execute(new HttpDelete(url)), responseClass);
  }

  protected <ResponseClass> ResponseClass get(String url, Class<ResponseClass> responseClass) throws IOException {
    return processResponse(client.execute(new HttpGet(url)), responseClass);
  }

  private <ResponseClass> ResponseClass processResponse(CloseableHttpResponse response, Class<ResponseClass> responseClass) throws IOException {
    try {
      if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() <= 299) {
        return objectMapper.readValue(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), responseClass);
      } else {
        throw new IOException("HTTP " + response.getStatusLine().getStatusCode());
      }
    } finally {
      response.close();
    }
  }


}
