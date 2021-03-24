package se.wikimedia.service.template;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import lombok.Getter;
import org.gwizard.services.Run;
import org.gwizard.swagger.SwaggerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.wikimedia.service.template.prevayler.PrevaylerModule;
import se.wikimedia.service.template.servlet.ServletModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author kalle
 * @since 2017-02-12 22:14
 */
public class Service {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Getter
  private String serviceName;

  public Service(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * All modules, including all service modules.
   */
  private List<Module> modules;

  /**
   * Modules aware of service name, containing initializable configurations, etc
   */
  private List<AbstractServiceModule> serviceModules;

  @Getter
  private Injector injector;

  public void run() {
    injector.getInstance(Run.class).start();
  }

  public void stop() {
    injector.getInstance(Run.class).stop();
  }

  public boolean open() throws Exception {

    modules = new ArrayList<>();

    modules.add(new ServiceModule(serviceName));

    modules.add(new ServletModule());
    modules.add(new SwaggerModule()); // depends on actions in ServletModule. binds to /swagger.json

    // begin templates
    // insert template modules here in order to have them initialized prior to user modules.

    // begin prevayler
    modules.add(new PrevaylerModule(serviceName));
    // end prevayler

    // end templates

    modules.addAll(getModules());

    serviceModules = new ArrayList<>();
    for (Module module : modules) {
      if (module instanceof AbstractServiceModule) {
        serviceModules.add((AbstractServiceModule) module);
      }
    }

    injector = Guice.createInjector(modules);

    List<Initializable> unopnenedInitializables = new ArrayList<>();

    for (AbstractServiceModule serviceModule : serviceModules) {
      for (Class<? extends Initializable> initializableClass : serviceModule.getInitializables()) {
        Initializable initializable = injector.getInstance(initializableClass);
        if (initializableClass.getAnnotation(Singleton.class) == null) {
          throw new RuntimeException("Initializable "+initializableClass.getName()+" is not annotated as Singleton!");
        }
        unopnenedInitializables.add(initializable);
      }
    }

    List<Initializable> openendInitializables = new ArrayList<>();
    long millisecondsTimeOut = TimeUnit.MINUTES.toMillis(1);
    long started = System.currentTimeMillis();
    while (!unopnenedInitializables.isEmpty()) {

      long millisecondsSpent = System.currentTimeMillis() - started;
      if (millisecondsSpent > millisecondsTimeOut) {
        log.error("Timeout opening initializables. Unable to open {}", unopnenedInitializables);

        // close any that we opened
        if (!openendInitializables.isEmpty()) {
          log.info("Closing any initializables that was opened...");
          for (Initializable initializable : openendInitializables) {
            if (!initializable.close()) {
              log.error("Unable to close " + initializable.toString());
            }
          }
        }

        return false;
      }

      for (Iterator<Initializable> iterator = unopnenedInitializables.iterator(); iterator.hasNext(); ) {
        Initializable initializable = iterator.next();
        if (initializable.open()) {
          iterator.remove();
          openendInitializables.add(initializable);
          log.info("Initialized " + initializable);
        } else {
          log.warn("Unable to initialize " + initializable);
        }
      }
    }

    long ended = System.currentTimeMillis();
    long millisecondsSpent = ended - started;
    log.info(millisecondsSpent + " milliseconds spent opening all initializables.");

    long bytesRamUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long mbRamUsed = bytesRamUsed / 1024 / 1024;
    log.info(mbRamUsed + " MB RAM in use.");

    return true;
  }

  public boolean close() throws Exception {

    List<Initializable> notClosedInitializables = new ArrayList<>();
    for (AbstractServiceModule serviceModule : serviceModules) {
      for (Class<? extends Initializable> initializableClass : serviceModule.getInitializables()) {
        notClosedInitializables.add(injector.getInstance(initializableClass));
      }
    }
    // close everything in reverse order that they where opened, in case of dependencies.
    Collections.reverse(notClosedInitializables);

    // todo retry for a while if unable

    boolean success = true;

    for (Iterator<Initializable> iterator = notClosedInitializables.iterator(); iterator.hasNext(); ) {
      Initializable initializable = iterator.next();
      try {
        if (initializable.close()) {
          iterator.remove();
        } else {
          log.error("Unable to close " + initializable.toString());
          success = false;
        }
      } catch (Exception e) {
        log.error("Caught exception trying to close {}", initializable, e);
        success = false;
      }
    }

    return success;
  }

  public List<Module> getModules() {
    return Collections.emptyList();
  }


}
