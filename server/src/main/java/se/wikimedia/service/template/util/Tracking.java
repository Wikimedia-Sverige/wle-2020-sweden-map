package se.wikimedia.service.template.util;

import java.util.UUID;

/**
 * @author kalle
 * @since 2017-09-29 00:32
 */
public class Tracking {

  public static final String httpHeader = "X-KODAPAN-TRACKING-IDENTITY";

  private Tracking() { }
  private static Tracking instance = new Tracking();
  public static Tracking getInstance() {
    return instance;
  }

  private final ThreadLocal<UUID> threadLocal = new ThreadLocal<>();

  public UUID get() {
    UUID identity = threadLocal.get();
    if (identity == null) {
      synchronized (threadLocal) {
        identity = threadLocal.get();
        if (identity == null) {
          identity = UUID.randomUUID();
          threadLocal.set(identity);
        }
      }
    }
    return identity;
  }

  public void set(UUID identity) {
    threadLocal.set(identity);
  }

}
