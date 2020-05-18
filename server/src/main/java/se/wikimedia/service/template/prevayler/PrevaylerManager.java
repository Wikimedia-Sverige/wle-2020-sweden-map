package se.wikimedia.service.template.prevayler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;
import lombok.Setter;
import org.prevayler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.wikimedia.service.template.Initializable;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static se.wikimedia.service.template.ServiceModule.SERVICE_DATA_PATH;

/**
 * @author kalle
 * @since 2019-02-26
 */
@Singleton
public class PrevaylerManager implements Initializable {

  private Logger log = LoggerFactory.getLogger(getClass());

  private Prevayler<Root> prevayler;

  private Lock lock = new ReentrantLock(true);

  @Inject
  @Named(SERVICE_DATA_PATH)
  private File serviceDataPath;

  @Override
  public boolean open() throws Exception {

    boolean success = true;

    if (prevayler == null) {
      File dataPath = new File(serviceDataPath, "prevayler");
      if (!dataPath.exists() && !dataPath.mkdirs()) {
        log.error("Unable to mkdirs {}", dataPath.getAbsolutePath());
        success = false;
      }
      if (!dataPath.isDirectory()) {
        log.error("{} is not a directory", dataPath);
        success = false;
      }
      if (success) {
        try {
          log.info("Starting Prevayler using path {}", dataPath.getAbsolutePath());
          prevayler = PrevaylerFactory.createPrevayler(new Root(), dataPath.getAbsolutePath());
        } catch (Exception e) {
          log.error("Exception caught creating Prevayler with path {}", dataPath.getAbsolutePath());
          success = false;
        }
      }
    }

    return success;
  }

  @Override
  public boolean close() throws Exception {
    boolean success = true;

    if (prevayler != null) {
      try {
        prevayler.close();
        prevayler = null;
      } catch (Exception e) {
        log.error("Exception caught while closing {}", prevayler);
        success = false;
      }
    }

    return success;
  }

  @Getter
  @Setter
  private long defaultTimeoutAmount = 10;

  @Getter
  @Setter
  private TimeUnit defaultTimeoutUnit = TimeUnit.MINUTES;

  public <R, E extends Exception> R execute(Command<R, E> command) throws E, InterruptedException, TimeoutException {
    return execute(command, defaultTimeoutAmount, defaultTimeoutUnit);
  }

  public <R, E extends Exception> R execute(Command<R, E> command, long timeoutAmount, TimeUnit timeoutUnit) throws E, InterruptedException, TimeoutException {
    if (lock.tryLock(timeoutAmount, timeoutUnit)) {
      try {
        return command.execute();
      } finally {
        lock.unlock();
      }
    }
    throw new TimeoutException("Unable to achieve command lock");
  }

  /**
   * Executes the given Transaction on the prevalentSystem(). ALL operations that alter the observable state of the prevalentSystem() must be implemented as Transaction or TransactionWithQuery objects and must be executed using the Prevayler.execute() methods. This method synchronizes on the prevalentSystem() to execute the Transaction. It is therefore guaranteed that only one Transaction is executed at a time. This means the prevalentSystem() does not have to worry about concurrency issues among Transactions.
   * Implementations of this interface can log the given Transaction for crash or shutdown recovery, for example, or execute it remotely on replicas of the prevalentSystem() for fault-tolerance and load-balancing purposes.
   *
   * @see org.prevayler.PrevaylerFactory
   */
  public void execute(Transaction<Root> transaction) {
    prevayler.execute(transaction);
  }

  /**
   * Executes the given sensitiveQuery on the prevalentSystem(). A sensitiveQuery is a Query that would be affected by the concurrent execution of a Transaction or other sensitiveQuery. This method synchronizes on the prevalentSystem() to execute the sensitiveQuery. It is therefore guaranteed that no other Transaction or sensitiveQuery is executed at the same time.
   * <br> Robust Queries (queries that do not affect other operations and that are not affected by them) can be executed directly as plain old method calls on the prevalentSystem() without the need of being implemented as Query objects. Examples of Robust Queries are queries that read the value of a single field or historical queries such as: "What was this account's balance at mid-night?".
   *
   * @return The result returned by the execution of the sensitiveQuery on the prevalentSystem().
   * @throws Exception The Exception thrown by the execution of the sensitiveQuery on the prevalentSystem().
   */
  public <R> R execute(Query<? super Root, R> sensitiveQuery) throws Exception {
    return prevayler.execute(sensitiveQuery);
  }

  /**
   * Executes the given transactionWithQuery on the prevalentSystem().
   * Implementations of this interface can log the given transaction for crash or shutdown recovery, for example, or execute it remotely on replicas of the prevalentSystem() for fault-tolerance and load-balancing purposes.
   *
   * @return The result returned by the execution of the transactionWithQuery on the prevalentSystem().
   * @throws Exception The Exception thrown by the execution of the sensitiveQuery on the prevalentSystem().
   * @see org.prevayler.PrevaylerFactory
   */
  public <R> R execute(TransactionWithQuery<? super Root, R> transactionWithQuery) throws Exception {
    return prevayler.execute(transactionWithQuery);
  }

  /**
   * The same as execute(TransactionWithQuery<P,R>) except no Exception is thrown.
   *
   * @return The result returned by the execution of the sureTransactionWithQuery on the prevalentSystem().
   */
  public <R> R execute(SureTransactionWithQuery<? super Root, R> sureTransactionWithQuery) {
    return prevayler.execute(sureTransactionWithQuery);
  }

  /**
   * Produces a complete serialized image of the underlying PrevalentSystem.
   * This will accelerate future system startups. Taking a snapshot once a day is enough for most applications.
   * This method synchronizes on the prevalentSystem() in order to take the snapshot. This means that transaction execution will be blocked while the snapshot is taken.
   *
   * @return The file to which the snapshot was written. This file should be left where it is, so that Prevayler can read it during startup. You can copy it to another location for backup purposes if desired.
   * @throws Exception if there is trouble writing to the snapshot file or serializing the prevalent system.
   */
  public File takeSnapshot() throws Exception {
    return prevayler.takeSnapshot();
  }


}
