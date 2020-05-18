package se.wikimedia.service.wle2020.naturvardsregistret.map.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class WikimediaImage implements Serializable {

  public static final long serialVersionUID = 1L;

  private String filename;

  private String thumburl;
  private Integer thumbwidth;
  private Integer thumbheight;
  private String url;
  private String descriptionurl;
  private String descriptionshorturl;

}
