package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Data
public class SetObjectWikidataEntryUpdated implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;
  private LocalDateTime wikidataEntryUpdated;

  public SetObjectWikidataEntryUpdated() {
  }

  public static SetObjectWikidataEntryUpdated factory(UUID identity, LocalDateTime wikidataEntryUpdated) {
    SetObjectWikidataEntryUpdated instance = new SetObjectWikidataEntryUpdated();
    instance.identity = identity;
    instance.wikidataEntryUpdated = wikidataEntryUpdated;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = root.getNaturvardsregistretObjects().get(identity);
    object.setWikidataEntryUpdated(wikidataEntryUpdated);
    return object;
  }
}
