package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.google.inject.Binder;
import se.wikimedia.service.wle2020.naturvardsregistret.map.index.NaturvardsregistretIndex;
import se.wikimedia.service.template.AbstractServiceModule;
import se.wikimedia.service.template.Initializable;

import java.util.ArrayList;
import java.util.List;

public class NaturvardsregistretMapModule extends AbstractServiceModule {

  public NaturvardsregistretMapModule(String serviceName) {
    super(serviceName);
  }

  @Override
  public List<Class<? extends Initializable>> getInitializables() {
    List<Class<? extends Initializable>> initializables = new ArrayList<>();

    // order is important due to dependencies
    initializables.add(Wikidata.class);
    initializables.add(WikimediaCommons.class);
    initializables.add(NaturvardsregistretIndex.class);
    initializables.add(NaturvardsregistretDataManager.class);
    initializables.add(NaturvardsregistretGeometryManager.class);

    return initializables;
  }

  @Override
  public void configure(Binder binder) {

  }
}
