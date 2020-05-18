package se.wikimedia.service.template;

import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * @author kalle
 * @since 2017-03-22
 */
public class Main {

  private static Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {


    List<Class<? extends Service>> serviceClasses = new ArrayList<>();

    ClassLoader cl = Main.class.getClassLoader();
    Set<ClassPath.ClassInfo> classesInPackage;

    try {
      classesInPackage = ClassPath.from(cl).getTopLevelClassesRecursive("se.wikimedia.service");
    } catch (IOException ioe) {
      log.error("Exception caught during search for Service implementation class", ioe);
      throw new RuntimeException(ioe);
    }
    for (ClassPath.ClassInfo classInfo : classesInPackage) {
      Class foundClass;
      try {
        foundClass = classInfo.load();
      } catch (NoClassDefFoundError e) {
        continue;
      }
      if (!foundClass.equals(Service.class) && Service.class.isAssignableFrom(foundClass)) {
        log.info("Found service class " + foundClass.getName());
        serviceClasses.add(foundClass);
      }

    }

    if (serviceClasses.isEmpty()) {
      log.error("Unable to find any implementation of Service in classpath. Unable to start.");

    } else if (serviceClasses.size() > 1) {
      log.error("Multiple implementations of Service found in classpath. Unable to start.");

    } else {

      Service service = serviceClasses.get(0).newInstance();

      start(service);

    }

  }

  public static void start(Service service) throws Exception {
    CountDownLatch stopSignal = new CountDownLatch(1);

    if (service.open()) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            service.close();
          } catch (Exception e) {
            log.error("Exception while closing service", e);
          } finally {
            stopSignal.countDown();
          }
        }
      });

      service.run();

      stopSignal.await();

    }
  }

}

