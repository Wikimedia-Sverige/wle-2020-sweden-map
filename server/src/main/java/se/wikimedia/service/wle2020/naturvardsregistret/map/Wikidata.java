package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import lombok.Getter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import se.wikimedia.service.template.Initializable;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
public class Wikidata implements Initializable {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  private ObjectMapper objectMapper;

  private CloseableHttpClient client;

  @Getter
  private WikibaseDataFetcher dataFetcher;

  private BasicApiConnection connection;

  private String userAgent = "wle2020-wmse-map";
  private String userAgentVersion = "0.1.0";
  //  private String username = "Karl Wettin (WMSE)";
//  private String password = "";
  private String emailAddress = "karl.wettin+bot@wikimedia.se";


  public Wikidata() {

  }

  @Override
  public boolean open() throws Exception {

    client = HttpClientBuilder.create().setUserAgent(userAgent + "/" + userAgentVersion + "(" + emailAddress + ")").build();

    connection = BasicApiConnection.getWikidataApiConnection();
//    connection.login(username, password);

    dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
    // Do not retrieve data that we don't care about here:
    dataFetcher.getFilter().excludeAllLanguages();
    dataFetcher.getFilter().excludeAllSiteLinks();

    return true;
  }


  @Override
  public boolean close() throws Exception {
    client.close();
    return true;
  }

  public PropertyIdValue property(String id) {
    return (PropertyIdValue) entity(id);
  }


  protected EntityIdValue entity(String id) {
    if (!id.trim().equals(id)) {
      log.warn("Whitespaces detected in '{}'", id, new RuntimeException("Developer typo!"));
      id = id.trim();
    }
    return EntityIdValueImpl.fromId(id, "http://www.wikidata.org/entity/");
  }

  public LocalDateTime toLocalDateTime(TimeValue timeValue) {
    timeValue = timeValue.toGregorian();
    StringBuilder sb = new StringBuilder(10);
    if (timeValue.getPrecision() >= TimeValue.PREC_YEAR) {
      sb.append(String.valueOf(timeValue.getYear()));
    }
    if (timeValue.getPrecision() >= TimeValue.PREC_MONTH) {
      if (sb.length() > 0) {
        sb.append("-");
      }
      sb.append(String.valueOf(timeValue.getMonth()));
    }
    if (timeValue.getPrecision() >= TimeValue.PREC_DAY) {
      if (sb.length() > 0) {
        sb.append("-");
      }
      sb.append(String.valueOf(timeValue.getDay()));
    }
    if (timeValue.getPrecision() >= TimeValue.PREC_HOUR) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(String.valueOf(timeValue.getHour()));
    }
    if (timeValue.getPrecision() >= TimeValue.PREC_MINUTE) {
      if (sb.length() > 0) {
        sb.append(":");
      }
      sb.append(String.valueOf(timeValue.getMinute()));
    }
    if (timeValue.getPrecision() >= TimeValue.PREC_SECOND) {
      if (sb.length() > 0) {
        sb.append(":");
      }
      sb.append(String.valueOf(timeValue.getSecond()));
    }
    DateTimeFormatter dateTimeFormatter;
    if (timeValue.getPrecision() == TimeValue.PREC_YEAR) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy")
          .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
          .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();
    } else if (timeValue.getPrecision() == TimeValue.PREC_MONTH) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-M")
          .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();
    } else if (timeValue.getPrecision() == TimeValue.PREC_DAY) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-M-d")
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();
    } else if (timeValue.getPrecision() == TimeValue.PREC_HOUR) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-M-d H")
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();
    } else if (timeValue.getPrecision() == TimeValue.PREC_MINUTE) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-M-d H:m")
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .toFormatter();
    } else if (timeValue.getPrecision() >= TimeValue.PREC_SECOND) {
      dateTimeFormatter = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-M-d H:m:s")
          .toFormatter();
    } else {
      throw new RuntimeException("Unsupported time value precision " + timeValue.getPrecision());
    }
    return LocalDateTime.parse(sb.toString(), dateTimeFormatter);
  }

  public ObjectNode query(String sparql) throws IOException {
    log.trace("Executing SPARQL query {}", sparql);

    String url = "http://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(sparql, "UTF8");

    CloseableHttpResponse response = client.execute(new HttpGet(url));
    try {
      if (response.getStatusLine().getStatusCode() != 200) {
        log.error("Wikidata response {}", response.getStatusLine());
        return null;
      }
      return objectMapper.readValue(response.getEntity().getContent(), ObjectNode.class);
    } finally {
      response.close();
    }

  }


  public Statement findMostRecentPublishedStatement(ItemDocument wikiDataItem, PropertyIdValue property) {
    StatementGroup statements = wikiDataItem.findStatementGroup(property);
    if (statements == null) {
      return null;
    }
    if (statements.size() > 1) {
      PropertyIdValue published = property("P577"); // published date
      Statement mostRecentStatement = null;
      LocalDateTime mostRecentStatementLocalDateTime = null;
      for (Statement statement : statements) {
        for (Reference reference : statement.getReferences()) {
          for (Iterator<Snak> snakIterator = reference.getAllSnaks(); snakIterator.hasNext(); ) {
            Snak snak = snakIterator.next();
            if (published.getId().equals(snak.getPropertyId().getId())) {
              TimeValue snakTimeValue = (TimeValue) snak.getValue();
              LocalDateTime snakDateTime = toLocalDateTime(snakTimeValue);
              if (mostRecentStatementLocalDateTime == null || snakDateTime.isAfter(mostRecentStatementLocalDateTime)) {
                mostRecentStatementLocalDateTime = snakDateTime;
                mostRecentStatement = statement;
                break;
              }
            }
          }
        }
        if (mostRecentStatement == null) {
          // in case no published qualifier, select the most recently added
          mostRecentStatement = statement;
        }
      }
      return mostRecentStatement;
    } else {
      return statements.getStatements().get(0);
    }
  }

  public ImageQueryResponse getImageMeta(String filename) throws Exception {
    // https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&iiprop=url&iiurlwidth=320&titles=File:T%C3%A5garps%20Hed.jpg
//
//      {
//        "batchcomplete": true,
//        "query": {
//          "pages": [{
//            "pageid": 28964850,
//            "ns": 6,
//            "title": "File:Tågarps Hed.jpg",
//            "imagerepository": "local",
//            "imageinfo": [{
//              "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/T%C3%A5garps_Hed.jpg/320px-T%C3%A5garps_Hed.jpg",
//              "thumbwidth": 320,
//              "thumbheight": 213,
//              "url": "https://upload.wikimedia.org/wikipedia/commons/9/9a/T%C3%A5garps_Hed.jpg",
//              "descriptionurl": "https://commons.wikimedia.org/wiki/File:T%C3%A5garps_Hed.jpg",
//              "descriptionshorturl": "https://commons.wikimedia.org/w/index.php?curid=28964850"
//            }]
//          }]
//        }
//      }
//

    URL url = new URL("https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&iiprop=url&iiurlwidth=320&titles=File:" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
    return objectMapper.readValue(url, ImageQueryResponse.class);
  }

  @Data
  public static class ImageQueryResponse {
    private boolean batchcomplete;
    private ImageQueryResponseQuery query;
  }

  @Data
  public static class ImageQueryResponseQuery {
    private List<ImageQueryResponseQueryPage> pages;
  }

  @Data
  public static class ImageQueryResponseQueryPage {
    private int pageid;
    private int ns;
    private String title;
    private String imagerepository;
    private List<ImageQueryResponseQueryPageImageInfo> imageinfo;
  }

  @Data
  public static class ImageQueryResponseQueryPageImageInfo {
    private String thumburl;
    private Integer thumbwidth;
    private Integer thumbheight;
    private String url;
    private String descriptionurl;
    private String descriptionshorturl;
  }

}
