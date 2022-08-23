package se.wikimedia.service.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.gwizard.web.WebConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.wikimedia.service.template.util.Environment;
import se.wikimedia.service.template.util.Tracking;

import javax.inject.Singleton;
import java.io.File;
import java.time.Clock;
import java.util.Collections;

/**
 * @author kalle
 * @since 2017-02-15 02:28
 */
public class ServiceModule extends AbstractServiceModule {

  private Logger log = LoggerFactory.getLogger(getClass());

  public static final String SERVICE_NAME = "service name";
  public static final String SERVICE_DATA_PATH = "service data path";
  public static final String SERVICE_DATA_PATH_PREFIX_ENV = "SERVICE_DATA_PATH_PREFIX_ENV";

  public ServiceModule(String serviceName) {
    super(serviceName);
  }

  private File serviceDataPath;

  @Override
  public void configure(Binder binder) {
    String serviceDataPathPrefix = Environment.getValue(SERVICE_DATA_PATH_PREFIX_ENV, (String)null);
    if (serviceDataPathPrefix == null) {
      log.error("Service data path prefix environment {} is not set.\nEnvironment.setDefaultValue(ServiceModule.SERVICE_DATA_PATH_PREFIX_ENV, \"/srv/project/\");", SERVICE_DATA_PATH_PREFIX_ENV);
      throw new RuntimeException("Service data path prefix environment '"+SERVICE_DATA_PATH_PREFIX_ENV+"' is not set.\nEnvironment.setDefaultValue(ServiceModule.SERVICE_DATA_PATH_PREFIX_ENV, \"/srv/project/\");");
    }
    serviceDataPath = new File(serviceDataPathPrefix);
    serviceDataPath = new File(serviceDataPath, getServiceName());
    if (!serviceDataPath.exists()) {
      log.info("Creating service path {}", serviceDataPath.getAbsolutePath());
      if (!serviceDataPath.mkdirs()) {
        log.error("Unable to mkdirs {}", serviceDataPath.getAbsolutePath());
        throw new RuntimeException("Unable to mkdirs " + serviceDataPath.getAbsolutePath());
      }
    }
    if (!serviceDataPath.isDirectory()) {
      log.error("Service data path {} is not a directory", serviceDataPath.getAbsolutePath());
      throw new RuntimeException("Service data path " + serviceDataPath.getAbsolutePath() + " is not a directory");
    }
  }

  @Override
  @Provides
  @Named(SERVICE_NAME)
  public String getServiceName() {
    return super.getServiceName();
  }

  @Provides
  @Named(SERVICE_DATA_PATH)
  public File getServiceDataPath() {
    return serviceDataPath;
  }


  public int getServerPort() {
    return Environment.getValue("webConfig.port", 8080);
  }

  @Singleton
  @Provides
  public WebConfig webConfigFactory() {
    WebConfig webConfig = new WebConfig();
    webConfig.setPort(getServerPort());
    return webConfig;
  }

  @Provides
  @Singleton
  public ObjectMapper objectMapperFactory() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Provides
  @Singleton
  public Clock clockFactory() {
    return Clock.systemDefaultZone();
  }

  @Provides
  public CloseableHttpClient httpClientFactory() {
    return HttpClientBuilder.create()
        .setDefaultHeaders(Collections.singleton(new BasicHeader(Tracking.httpHeader, String.valueOf(Tracking.getInstance().get()))))
        .build();
  }

}
