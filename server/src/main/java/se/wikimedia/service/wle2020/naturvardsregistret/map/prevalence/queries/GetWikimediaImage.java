package se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries;

import org.prevayler.Query;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.WikimediaImage;

import java.util.Date;

public class GetWikimediaImage implements Query<Root, WikimediaImage> {

  private String filename;

  public GetWikimediaImage(String filename) {
    this.filename = filename;
  }

  @Override
  public WikimediaImage query(Root root, Date date) throws Exception {
    return root.getWikimediaImages().get(filename);
  }
}
