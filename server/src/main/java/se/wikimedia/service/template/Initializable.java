package se.wikimedia.service.template;

/**
 * All registered initializables must be @Singleton
 * @author kalle
 * @since 2017-02-15 02:24
 */
public interface Initializable {

  /**
   *
   * @return true if already opened or succeeded to open
   * @throws Exception
   */
  public abstract boolean open() throws Exception;

  /**
   *
   * @return true if already closed or succeeded to close
   * @throws Exception
   */
  public abstract boolean close() throws Exception;

}
