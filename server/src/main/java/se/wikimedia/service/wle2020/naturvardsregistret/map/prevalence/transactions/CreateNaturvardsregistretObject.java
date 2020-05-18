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
public class CreateNaturvardsregistretObject implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;

  private String wikidataQ;
  private Integer naturvardsregistretIdentity;
  private LocalDateTime wikidataEntryUpdated;

  public CreateNaturvardsregistretObject() {
  }

  public static CreateNaturvardsregistretObject factory(String wikidataQ, Integer naturvardsregistretIdentity, LocalDateTime wikidataEntryUpdated) {
    CreateNaturvardsregistretObject instance = new CreateNaturvardsregistretObject();
    instance.identity = UUID.randomUUID();
    instance.wikidataQ = wikidataQ;
    instance.naturvardsregistretIdentity = naturvardsregistretIdentity;
    instance.wikidataEntryUpdated = wikidataEntryUpdated;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = new NaturvardsregistretObject();
    object.setIdentity(identity);
    object.setWikidataQ(wikidataQ);
    object.setNaturvardsregistretIdentity(naturvardsregistretIdentity);
    object.setWikidataEntryUpdated(wikidataEntryUpdated);

    root.getNaturvardsregistretObjects().put(identity, object);
    root.getNaturvardsregistretObjectsByQ().put(wikidataQ, object);

    return object;
  }

}
