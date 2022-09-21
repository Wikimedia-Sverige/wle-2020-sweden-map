package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.sourceforge.jwbf.core.contentRep.Article;
import org.prevayler.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.*;
import se.wikimedia.service.template.Initializable;
import se.wikimedia.service.template.prevayler.PrevaylerManager;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;
import se.wikimedia.service.wle2020.naturvardsregistret.map.index.NaturvardsregistretIndex;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries.GetObjectByQ;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries.GetPreviousSuccessfulNoValuesPollStarted;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries.GetPreviousSuccessfulPollStarted;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries.GetWikimediaImage;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.transactions.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Singleton
public class NaturvardsregistretDataManager implements Initializable {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private PrevaylerManager prevayler;

  @Inject
  private Wikidata wikidata;

  @Inject
  private WikimediaCommons commons;

  @Inject
  private NaturvardsregistretIndex index;

  @Inject
  private Clock clock;

  private boolean stopSignal = false;
  private CountDownLatch stoppedSignal;

  private void sleep(long millis) throws InterruptedException {
    long end = System.currentTimeMillis() + millis;
    while (!stopSignal && System.currentTimeMillis() < end) {
      Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    }
  }

  @Override
  public boolean open() throws Exception {
    index.reconstruct();

    long numberOfJsonChars = prevayler.execute(new Query<Root, Long>() {
      @Override
      public Long query(Root root, Date executionTime) throws Exception {
        long numberOfJsonChars = 0;
        for (NaturvardsregistretObject object : root.getNaturvardsregistretObjects().values())
          if (object.getFeatureGeometry() != null)
            numberOfJsonChars += object.getFeatureGeometry().length();
        return numberOfJsonChars;
      }
    });
    log.info("{} JSON feature geometry characters in RAM", numberOfJsonChars);

    // make sure to update this number if you modify number of threads!
    stoppedSignal = new CountDownLatch(4);

    Thread pollUpdatedWikidataEntriesThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stopSignal) {
          try {
            poll();
            sleep(TimeUnit.MINUTES.toMillis(5));
          } catch (Exception e) {
            log.error("Caught exception in poller thread", e);
          }
        }
        log.info("Wikidata updated poller thread ends.");
        stoppedSignal.countDown();
      }
    });
    pollUpdatedWikidataEntriesThread.setDaemon(true);
    pollUpdatedWikidataEntriesThread.setName("Poller");
    pollUpdatedWikidataEntriesThread.start();

    Thread wikimediaImagesFetcherThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stopSignal) {
          try {
            updateWikimediaImages();
            sleep(TimeUnit.MINUTES.toMillis(1));
          } catch (Exception e) {
            log.error("Caught exception in Wikimedia image fetcher thread", e);
          }
        }
        log.info("Wikimedia image fetcher thread ends.");
        stoppedSignal.countDown();
      }
    });
    wikimediaImagesFetcherThread.setDaemon(true);
    wikimediaImagesFetcherThread.setName("Image fetcher");
    wikimediaImagesFetcherThread.start();

    Thread fetchUpdatedWikidataEntriesThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stopSignal) {
          try {
            List<NaturvardsregistretObject> objectsToUpdate = prevayler.execute(new Query<Root, List<NaturvardsregistretObject>>() {
              @Override
              public List<NaturvardsregistretObject> query(Root root, Date date) throws Exception {
                List<NaturvardsregistretObject> response = new ArrayList<>();
                for (NaturvardsregistretObject object : root.getNaturvardsregistretObjects().values()) {
                  if (object.getUpdatedFromWikidata() == null
                      || object.getUpdatedFromWikidata().isBefore(object.getWikidataEntryUpdated())) {
                    response.add(object);
                  }
                }
                return response;
              }
            });
            if (objectsToUpdate.size() > 0) {
              log.info("{} objects to be updated from Wikidata...", objectsToUpdate.size());
            }
            for (NaturvardsregistretObject object : objectsToUpdate) {
              updateObjectFromWikidata(object);
              if (stopSignal) {
                break;
              }
            }
            sleep(TimeUnit.SECONDS.toMillis(30));
          } catch (Exception e) {
            log.error("Caught exception in Wikidata fetcher thread", e);
          }
        }
        log.info("Wikidata fetcher thread ends.");
        stoppedSignal.countDown();
      }
    });
    fetchUpdatedWikidataEntriesThread.setDaemon(true);
    fetchUpdatedWikidataEntriesThread.setName("Wikidata fetcher");
    fetchUpdatedWikidataEntriesThread.start();

    Thread fetchCommonsMapThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stopSignal) {
          try {
            List<NaturvardsregistretObject> objectsToUpdate = prevayler.execute(new Query<Root, List<NaturvardsregistretObject>>() {
              @Override
              public List<NaturvardsregistretObject> query(Root root, Date date) throws Exception {
                List<NaturvardsregistretObject> response = new ArrayList<>();
                for (NaturvardsregistretObject object : root.getNaturvardsregistretObjects().values()) {
                  if (object.getCommonsMapPath() != null && object.getUpdatedFromCommons() == null) {
                    response.add(object);
                  }
                }
                return response;
              }
            });
            if (objectsToUpdate.size() > 0) {
              log.info("{} objects to be updated from Commons...", objectsToUpdate.size());
            }
            for (NaturvardsregistretObject object : objectsToUpdate) {
              updateObjectFromCommons(object);
              if (stopSignal) {
                break;
              }
            }
            sleep(TimeUnit.SECONDS.toMillis(30));
          } catch (Exception e) {
            log.error("Caught exception in Commons fetcher thread", e);
          }
        }
        log.info("Commons fetcher thread ends.");
        stoppedSignal.countDown();
      }
    });
    fetchCommonsMapThread.setDaemon(true);
    fetchCommonsMapThread.setName("Commons fetcher");
    fetchCommonsMapThread.start();


    return true;
  }

  @Override
  public boolean close() throws Exception {
    stopSignal = true;
    log.info("Waiting for threads to end...");
    stoppedSignal.await();
    return true;
  }

  private void poll() throws Exception {

    log.info("Polling Wikidata for updated items");


    LocalDateTime started = LocalDateTime.now(clock);

    // items with nvrid value set
    {
      LocalDateTime previousSuccessfulPollStarted = prevayler.execute(new GetPreviousSuccessfulPollStarted());
      if (previousSuccessfulPollStarted == null) {
        previousSuccessfulPollStarted = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
      }

      String sparql = "SELECT ?item ?nvrid (STR(?date_modified) AS ?iso_timestamp) WHERE {\n" +
          "  ?item wdt:P3613 ?nvrid;\n" +
          "  schema:dateModified ?date_modified.\n" +
          "  FILTER(?date_modified > \"" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(previousSuccessfulPollStarted) + "Z\"^^xsd:dateTime)\n" +
          "}";
      if (poll(sparql) > 0) {
        prevayler.execute(SetPreviousSuccessfulPollStarted.factory(started));
      } else {
        log.debug("No updated Wikidata entities found.");
      }

    }

    // items with no nvrid value set
    {
      LocalDateTime previousSuccessfulNoValuesPollStarted = prevayler.execute(new GetPreviousSuccessfulNoValuesPollStarted());
      if (previousSuccessfulNoValuesPollStarted == null) {
        previousSuccessfulNoValuesPollStarted = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
      }

      String sparql = "SELECT ?item (STR(?date_modified) AS ?iso_timestamp) WHERE {\n" +
          "  ?item rdf:type wdno:P3613;\n" +
          "  schema:dateModified ?date_modified.\n" +
          "  FILTER(?date_modified > \"" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(previousSuccessfulNoValuesPollStarted) + "Z\"^^xsd:dateTime)\n" +
          "}";
      if (poll(sparql) > 0) {
        prevayler.execute(SetPreviousSuccessfulNoValuesPollStarted.factory(started));
      } else {
        log.debug("No updated Wikidata entities found.");
      }

    }

  }

  private int poll(String sparql) throws Exception {
    ObjectNode response = wikidata.query(sparql);
    ArrayNode bindings = (ArrayNode) response.get("results").get("bindings");
    log.trace("Found {} Wikidata items that has been touched since previous poll", bindings.size());
    for (int i = 0; i < bindings.size(); i++) {
      if (stopSignal) {
        return 0;
      }
      ObjectNode binding = (ObjectNode) bindings.get(i);
      String q = binding.get("item").get("value").textValue().replaceFirst("^(.+)(Q\\d+)$", "$2");
      String nvrid;
      if (binding.has("nvrid")) {
       nvrid = binding.get("nvrid").get("value").textValue();
      } else {
        nvrid = null;
      }
      LocalDateTime updated = LocalDateTime.parse(binding.get("iso_timestamp").get("value").textValue().substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

      NaturvardsregistretObject object = prevayler.execute(new GetObjectByQ(q));
      if (object != null) {
        if (object.getWikidataEntryUpdated() == null || object.getWikidataEntryUpdated().isBefore(updated)) {
          log.info("{} has been updated since previous successful poll", q);
          prevayler.execute(SetObjectWikidataEntryUpdated.factory(object.getIdentity(), updated));
        }
      } else {
        log.info("{} has been created since previous successful poll", q);
        object = prevayler.execute(CreateNaturvardsregistretObject.factory(q, nvrid, updated));
      }


    }
    return bindings.size();
  }

  private void updateWikimediaImages() throws Exception {
    Set<String> mentionedNonExistingWikimediaImages = prevayler.execute(new Query<Root, Set<String>>() {
      @Override
      public Set<String> query(Root root, Date date) throws Exception {
        Set<String> response = new HashSet<>();
        for (NaturvardsregistretObject object : root.getNaturvardsregistretObjects().values()) {
          if (object.getWikidataImageNames() != null) {
            for (String filename : object.getWikidataImageNames()) {
              if (!root.getWikimediaImages().containsKey(filename)) {
                response.add(filename);
              }
            }
          }
        }
        return response;
      }
    });
    if (!mentionedNonExistingWikimediaImages.isEmpty()) {
      log.info("{} mentioned Wikimedia images to be processed...", mentionedNonExistingWikimediaImages.size());
      for (String filename : mentionedNonExistingWikimediaImages) {
        updateWikimediaImage(filename);
      }
    }
  }

  private void updateWikimediaImage(String filename) throws Exception {
    log.info("Processing Wikimedia image {}", filename);
    Wikidata.ImageQueryResponse imageQueryResponse = wikidata.getImageMeta(filename);
    Wikidata.ImageQueryResponseQueryPageImageInfo imageInfo = imageQueryResponse.getQuery().getPages().get(0).getImageinfo().get(0);
    prevayler.execute(CreateWikimediaImage.factory(
        filename,
        imageInfo.getThumburl(),
        imageInfo.getThumbwidth(),
        imageInfo.getThumbheight(),
        imageInfo.getUrl(),
        imageInfo.getDescriptionurl(),
        imageInfo.getDescriptionshorturl()
    ));

  }

  private void updateObjectFromWikidata(NaturvardsregistretObject object) throws Exception {
    // download from wikidata

    log.debug("Downloading and processing Wikidata object {}", object.getWikidataQ());

    String q = object.getWikidataQ();
    EntityDocument entityDocument = wikidata.getDataFetcher().getEntityDocument(q);
    ItemDocument itemDocument = (ItemDocument) entityDocument;

    {
      Statement geoshapeStatement = wikidata.findMostRecentPublishedStatement(itemDocument, wikidata.property("P3896"));
      if (geoshapeStatement != null) {
        StringValue geoshapeValue = (StringValue) geoshapeStatement.getMainSnak().getValue();
        String commonsGeoshapeUrl = geoshapeValue.getString();
        if (!commonsGeoshapeUrl.equals(object.getCommonsMapPath())) {
          prevayler.execute(SetObjectCommonsMapPath.factory(object.getIdentity(), commonsGeoshapeUrl));
        }
      }
    }

    {
      String stereotype = null;
      StatementGroup instanceOf = itemDocument.findStatementGroup(wikidata.property("P31"));
      if (instanceOf != null) {

        EntityIdValue natureReserve = wikidata.entity("Q179049");
        EntityIdValue nationalPark = wikidata.entity("Q46169");
        EntityIdValue biosphereReserve = wikidata.entity("Q158454");
        EntityIdValue naturalMonument = wikidata.entity("Q23790");

        for (Statement statement : instanceOf) {
          System.currentTimeMillis();
          if (natureReserve.equals(statement.getMainSnak().getValue())) {
            stereotype = "nature reserve";
            break;
          } else if (nationalPark.equals(statement.getMainSnak().getValue())) {
            stereotype = "national park";
            break;
          } else if (biosphereReserve.equals(statement.getMainSnak().getValue())) {
            stereotype = "biosphere reserve";
            break;
          } else if (naturalMonument.equals(statement.getMainSnak().getValue())) {
            stereotype = "natural monument";
            break;
          }
        }
      }
      if (stereotype == null) {
        if (itemDocument.findStatementGroup(wikidata.property("P1435")) != null) {
          // natural monument
          stereotype = "natural monument";
        }
      }

      if (stereotype == null) {
        log.error("Unable to extract supported stereotype from {}", object.getWikidataQ());
      } else {
        if (!stereotype.equals(object.getStereotype())) {
          prevayler.execute(SetObjectStereotype.factory(object.getIdentity(), stereotype));
        }
      }
    }

    {
      String label = itemDocument.getLabels().get("sv").getText();
      if (!label.equals(object.getWikidataLabel())) {
        prevayler.execute(SetObjectLabel.factory(object.getIdentity(), label));
      }
    }

    {
      Set<String> existingImageNames = new HashSet<>();
      StatementGroup imageStatementGroup = itemDocument.findStatementGroup(wikidata.property("P18"));
      if (imageStatementGroup != null && !imageStatementGroup.isEmpty()) {
        for (Statement imageStatement : imageStatementGroup) {
          StringValue imageStringValue = (StringValue) imageStatement.getValue();
          String imageName = imageStringValue.getString();
          existingImageNames.add(imageName);
        }
      }
      Set<String> removedImages = new HashSet<>(object.getWikidataImageNames());
      removedImages.removeAll(existingImageNames);
      if (!removedImages.isEmpty()) {
        prevayler.execute(RemoveObjectImages.factory(object.getIdentity(), removedImages));
      }

      Set<String> newImages = new HashSet<>(existingImageNames);
      newImages.removeAll(object.getWikidataImageNames());
      if (!newImages.isEmpty()) {
        prevayler.execute(AddObjectImages.factory(object.getIdentity(), newImages));
      }
    }

    for (String filename : object.getWikidataImageNames()) {
      if (prevayler.execute(new GetWikimediaImage(filename)) == null) {
        updateWikimediaImage(filename);
      }
    }


    prevayler.execute(SetObjectUpdatedFromWikidata.factory(object.getIdentity(), LocalDateTime.now(clock)));
  }

  private void updateObjectFromCommons(NaturvardsregistretObject object) throws Exception {

    if (object.getCommonsMapPath() == null) {
      log.warn("Commons map URL is missing in object {} representing {}", object.getIdentity(), object.getWikidataQ());
      return;
    }

    log.debug("Downloading and processing Commons article {}", object.getCommonsMapPath());

    Article article = commons.getBot().getArticle(object.getCommonsMapPath());
    if (!article.getRevisionId().equals(object.getCommonsMapRevisionId())) {
      ObjectNode existingCommonsObject = objectMapper.readValue(article.getText(), ObjectNode.class);
      ObjectNode geometry = (ObjectNode) existingCommonsObject.get("data").get("geometry");
      String json = objectMapper.writeValueAsString(geometry);
      prevayler.execute(SetObjectFeatureGeometry.factory(object.getIdentity(), article.getRevisionId(), json));
      index.update(object);
    }

    prevayler.execute(SetObjectUpdatedFromCommons.factory(object.getIdentity(), LocalDateTime.now(clock)));


  }

}
