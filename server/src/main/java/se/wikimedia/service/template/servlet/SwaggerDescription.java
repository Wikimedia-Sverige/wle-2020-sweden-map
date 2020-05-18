package se.wikimedia.service.template.servlet;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;

/**
 * @author kalle
 * @since 2017-08-16
 */
@SwaggerDefinition
public class SwaggerDescription implements ReaderListener {

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {
  }

  @Override
  public void afterScan(Reader reader, Swagger swagger) {
    swagger.getInfo().setTitle("Service");
    swagger.getInfo().setVersion("Version");
  }
}
