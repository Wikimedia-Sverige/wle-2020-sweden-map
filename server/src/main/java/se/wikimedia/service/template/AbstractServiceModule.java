package se.wikimedia.service.template;

import com.google.inject.Module;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * @author kalle
 * @since 2019-02-26
 */
public abstract class AbstractServiceModule implements Module {

  @Getter
  private String serviceName;

  public AbstractServiceModule(String serviceName) {
    this.serviceName = serviceName;
  }

  public List<Class<? extends Initializable>> getInitializables() {
    return Collections.emptyList();
  }

}
