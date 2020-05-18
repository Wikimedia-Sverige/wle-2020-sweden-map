package se.wikimedia.service.template.prevayler;

import lombok.Data;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.WikimediaImage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author kalle
 * @since 2019-01-29
 */
@Data
public class Root implements Serializable {

  public static final long serialVersionUID = 1L;

  private LocalDateTime previousSuccessfulPollStarted = null;
  private LocalDateTime previousSuccessfulNoValuesPollStarted = null;

  private Map<UUID, NaturvardsregistretObject> naturvardsregistretObjects = new HashMap<>();
  private Map<String, NaturvardsregistretObject> naturvardsregistretObjectsByQ = new HashMap<>();

  private Map<String, WikimediaImage> wikimediaImages = new HashMap<>();

}
