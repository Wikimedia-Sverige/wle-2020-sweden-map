package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class SetObjectLabel implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;
  private String label;

  public SetObjectLabel() {
  }

  public static SetObjectLabel factory(UUID identity, String label) {
    SetObjectLabel instance = new SetObjectLabel();
    instance.identity = identity;
    instance.label = label;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = root.getNaturvardsregistretObjects().get(identity);
    object.setWikidataLabel(label);
    return object;
  }
}
