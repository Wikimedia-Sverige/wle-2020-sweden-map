package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Data
public class AddObjectImages implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;
  private Collection<String> wikidataImageNames;

  public AddObjectImages() {
  }

  public static AddObjectImages factory(UUID identity, Collection<String> wikidataImageNames) {
    AddObjectImages instance = new AddObjectImages();
    instance.identity = identity;
    instance.wikidataImageNames = wikidataImageNames;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = root.getNaturvardsregistretObjects().get(identity);
    object.getWikidataImageNames().addAll(wikidataImageNames);
    return object;
  }
}
