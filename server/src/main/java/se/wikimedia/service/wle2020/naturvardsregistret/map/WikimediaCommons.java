package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.google.inject.Singleton;
import lombok.Getter;
import net.sourceforge.jwbf.core.actions.HttpActionClient;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import se.wikimedia.service.template.Initializable;

import java.util.concurrent.TimeUnit;

@Singleton
public class WikimediaCommons implements Initializable {

  @Getter
  private MediaWikiBot bot;

  private String userAgent = "wle2020-wmse-map";
  private String userAgentVersion = "0.1.0";
  //  private String username = "Karl Wettin (WMSE)";
//  private String password = "";
  private String emailAddress = "karl.wettin+bot@wikimedia.se";

  @Override
  public boolean open() throws Exception {

    HttpActionClient client = HttpActionClient.builder() //
        .withUrl("https://commons.wikimedia.org/w/") //
        .withUserAgent(userAgent, userAgentVersion, emailAddress) //
        .withRequestsPerUnit(60, TimeUnit.MINUTES) //
        .build();

    bot = new MediaWikiBot(client);

    return true;
  }

  @Override
  public boolean close() throws Exception {
    return true;
  }
}
