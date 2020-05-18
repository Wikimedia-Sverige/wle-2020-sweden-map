package se.wikimedia.service.template;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import se.wikimedia.service.template.util.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Just starts it up and close it down.
 *
 * @author kalle
 * @since 2017-03-07 22:06
 */
public class TestService extends ServiceTest {

  @Test
  public void test() throws Exception {

    File tmp = File.createTempFile("test-data", "dir");
    FileUtils.deleteQuietly(tmp);
    if (!tmp.mkdirs()) {
      throw new IOException("Unable to mkdirs " + tmp.getAbsolutePath());
    }
    try {

      Environment.setDefaultValue(ServiceModule.SERVICE_DATA_PATH_PREFIX_ENV, tmp.getAbsolutePath());

      int webConfigPort = 9000 + new Random().nextInt(1000);
      Environment.setDefaultValue("webConfig.port", webConfigPort);

      Service service = new Service("test-" + System.currentTimeMillis());

      Assert.assertTrue(service.open());
      try {
        service.run();
        // todo assert ping answers
        service.stop();
      } finally {
        Assert.assertTrue(service.close());
      }

    } finally {
      FileUtils.deleteQuietly(tmp);
    }
  }

}
