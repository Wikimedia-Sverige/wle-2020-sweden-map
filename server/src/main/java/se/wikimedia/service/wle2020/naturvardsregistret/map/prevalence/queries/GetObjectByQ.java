package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries;

import org.prevayler.Query;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.util.Date;

public class GetObjectByQ implements Query<Root, NaturvardsregistretObject> {

  private String q;

  public GetObjectByQ(String q) {
    this.q = q;
  }

  @Override
  public NaturvardsregistretObject query(Root root, Date date) throws Exception {
    return root.getNaturvardsregistretObjectsByQ().get(q);
  }
}
