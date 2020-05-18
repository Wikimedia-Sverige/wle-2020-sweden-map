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
public class SetObjectUpdatedFromWikidata implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;
  private LocalDateTime when;

  public SetObjectUpdatedFromWikidata() {
  }

  public static SetObjectUpdatedFromWikidata factory(UUID identity, LocalDateTime when) {
    SetObjectUpdatedFromWikidata instance = new SetObjectUpdatedFromWikidata();
    instance.identity = identity;
    instance.when = when;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = root.getNaturvardsregistretObjects().get(identity);
    object.setUpdatedFromWikidata(when);
    return object;
  }
}
