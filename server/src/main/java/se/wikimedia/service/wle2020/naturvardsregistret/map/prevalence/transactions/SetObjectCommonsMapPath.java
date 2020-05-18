package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class SetObjectCommonsMapPath implements TransactionWithQuery<Root, NaturvardsregistretObject>, Serializable {

  public static final long serialVersionUID = 1L;

  private UUID identity;
  private String commonsMapPath;

  public SetObjectCommonsMapPath() {
  }

  public static SetObjectCommonsMapPath factory(UUID identity, String commonsMapPath) {
    SetObjectCommonsMapPath instance = new SetObjectCommonsMapPath();
    instance.identity = identity;
    instance.commonsMapPath = commonsMapPath;
    return instance;
  }

  @Override
  public NaturvardsregistretObject executeAndQuery(Root root, Date date) throws Exception {
    NaturvardsregistretObject object = root.getNaturvardsregistretObjects().get(identity);
    object.setCommonsMapPath(commonsMapPath);
    return object;
  }
}
