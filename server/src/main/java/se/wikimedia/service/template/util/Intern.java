package se.wikimedia.service.template.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kalle
 * @since 2017-02-22 00:32
 */
public class Intern<T> {

  private Map<T, T> map;

  public Intern() {
    this(100);
  }

  public Intern(int initialCapacity) {
    map = new HashMap<T, T>();
  }

  public T intern(T object) {
    T interned = map.get(object);
    if (interned == null) {
      map.put(object, object);
      interned = object;
    }
    return interned;
  }

}
