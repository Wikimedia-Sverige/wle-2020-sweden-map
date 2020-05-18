package se.wikimedia.service.template.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kalle
 * @since 2017-02-22 00:29
 */
public class Environment {

  /** Mainly for unit tests */
  private static Map<String, String> defaultValues = new HashMap<>();

  public static void setDefaultValue(String key, String value) {
    defaultValues.put(key, value);
  }

  public static void setDefaultValue(String key, Integer value) {
    defaultValues.put(key, String.valueOf(value));
  }

  public static void setDefaultValue(String key, Boolean value) {
    defaultValues.put(key, String.valueOf(value));
  }

  public static String getValue(String key, String defaultValue) {
    String value = System.getenv(key);
    if (value == null) {
      value = defaultValues.get(key);
    }
    return value != null ? value : defaultValue;
  }

  public static Integer getValue(String key, Integer defaultValue) {
    String value = System.getenv(key);
    if (value == null) {
      value = defaultValues.get(key);
    }
    return value != null ? Integer.valueOf(value) : defaultValue;
  }

  public static Boolean getValue(String key, Boolean defaultValue) {
    String value = System.getenv(key);
    if (value == null) {
      value = defaultValues.get(key);
    }
    return value != null ? Boolean.valueOf(value) : defaultValue;
  }


}
