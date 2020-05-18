package se.wikimedia.service.template.prevayler;

import com.google.inject.*;
import se.wikimedia.service.template.AbstractServiceModule;
import se.wikimedia.service.template.Initializable;

import java.util.Collections;
import java.util.List;


/**
 * @author kalle
 * @since 2019-01-29
 */
public class PrevaylerModule extends AbstractServiceModule {

  public PrevaylerModule(String serviceName) {
    super(serviceName);
  }

  @Override
  public void configure(Binder binder) {

  }

  @Override
  public List<Class<? extends Initializable>> getInitializables() {
    return Collections.singletonList(PrevaylerManager.class);
  }
}
