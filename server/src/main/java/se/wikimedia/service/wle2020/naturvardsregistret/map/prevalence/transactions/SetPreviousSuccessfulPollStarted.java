package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.Transaction;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class SetPreviousSuccessfulPollStarted implements Transaction<Root>, Serializable {

  public static final long serialVersionUID = 1L;

  private LocalDateTime previousSuccessfulPollStarted;

  public SetPreviousSuccessfulPollStarted() {
  }

  public static SetPreviousSuccessfulPollStarted factory(LocalDateTime previousSuccessfulPollStarted) {
    SetPreviousSuccessfulPollStarted instance = new SetPreviousSuccessfulPollStarted();
    instance.previousSuccessfulPollStarted = previousSuccessfulPollStarted;
    return instance;
  }

  @Override
  public void executeOn(Root root, Date date) {
    root.setPreviousSuccessfulPollStarted(previousSuccessfulPollStarted);
  }
}
