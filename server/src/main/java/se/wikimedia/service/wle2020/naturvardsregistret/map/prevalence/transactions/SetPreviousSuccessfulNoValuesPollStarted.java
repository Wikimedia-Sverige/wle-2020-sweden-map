package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.Transaction;
import se.wikimedia.service.template.prevayler.Root;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class SetPreviousSuccessfulNoValuesPollStarted implements Transaction<Root>, Serializable {

  public static final long serialVersionUID = 1L;

  private LocalDateTime previousSuccessfulNoValuesPollStarted;

  public SetPreviousSuccessfulNoValuesPollStarted() {
  }

  public static SetPreviousSuccessfulNoValuesPollStarted factory(LocalDateTime previousSuccessfulPollStarted) {
    SetPreviousSuccessfulNoValuesPollStarted instance = new SetPreviousSuccessfulNoValuesPollStarted();
    instance.previousSuccessfulNoValuesPollStarted = previousSuccessfulPollStarted;
    return instance;
  }

  @Override
  public void executeOn(Root root, Date date) {
    root.setPreviousSuccessfulNoValuesPollStarted(previousSuccessfulNoValuesPollStarted);
  }
}
