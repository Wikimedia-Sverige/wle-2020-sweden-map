package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.google.inject.Module;
import se.wikimedia.service.template.Main;
import se.wikimedia.service.template.Service;
import se.wikimedia.service.template.util.Environment;

import java.util.Collections;
import java.util.List;

public class NaturvardsregistretMap extends Service {

  public static void main(String... args) throws Exception {
    Main.start(new NaturvardsregistretMap());
  }

  public NaturvardsregistretMap() {
    super("naturvardsregistret-map");
    Environment.setDefaultValue("service-data-path-prefix", "/srv");
  }

  @Override
  public List<Module> getModules() {
    return Collections.singletonList(new NaturvardsregistretMapModule(getServiceName()));
  }

}
