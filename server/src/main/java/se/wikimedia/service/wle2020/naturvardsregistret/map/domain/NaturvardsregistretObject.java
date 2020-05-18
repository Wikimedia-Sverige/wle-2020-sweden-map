package se.wikimedia.service.wle2020.naturvardsregistret.map.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class NaturvardsregistretObject implements Serializable {

  public static final long serialVersionUID = 1L;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NaturvardsregistretObject naturvardsregistretObject = (NaturvardsregistretObject) o;
    return Objects.equals(identity, naturvardsregistretObject.identity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identity);
  }

  private UUID identity;

  private String stereotype;

  private String wikidataQ;
  private Integer naturvardsregistretIdentity;

  private String wikidataLabel;

  private String featureGeometry;

  private String commonsMapPath;
  private String commonsMapRevisionId;

  private List<String> wikidataImageNames = new ArrayList<>();

  /** Timestamp this object was last updated according to Wikidata */
  private LocalDateTime wikidataEntryUpdated;

  private LocalDateTime updatedFromWikidata;
  private LocalDateTime updatedFromCommons;

}
