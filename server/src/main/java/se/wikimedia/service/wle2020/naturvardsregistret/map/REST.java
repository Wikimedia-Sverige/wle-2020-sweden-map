package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.prevayler.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.Point;
import se.wikimedia.service.template.prevayler.PrevaylerManager;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.WikimediaImage;
import se.wikimedia.service.wle2020.naturvardsregistret.map.index.BoundingBox;
import se.wikimedia.service.wle2020.naturvardsregistret.map.index.NaturvardsregistretIndex;
import se.wikimedia.service.wle2020.naturvardsregistret.map.prevalence.queries.GetWikimediaImage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class REST {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  private PrevaylerManager prevayler;

  @Inject
  private NaturvardsregistretIndex index;

  @Inject
  private NaturvardsregistretGeometryManager geometryManager;

  @Inject
  private ObjectMapper objectMapper;

  @Data
  public static class SearchResultDTO {
    private UUID identity;
    private Integer nvrid;
    private String stereotype;
    private Feature feature;
    private Point centroid;
    private String label;
    private List<SearchResultImageDTO> images = new ArrayList<>();
    private String q;
  }

  @Data
  public static class SearchResultImageDTO {
    private String url;
    private String filename;
    private Integer width;
    private Integer height;
  }

  private Comparator<SearchResultDTO> searchResultDTOComparator = new Comparator<SearchResultDTO>() {

    private Map<String, Integer> order = Stream.of(new Object[][]{
        {"biosphere reserve", 1},
        {"national park", 2},
        {"nature reserve", 3},
        {"natural monument", 4},
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));

    @Override
    public int compare(SearchResultDTO dto1, SearchResultDTO dto2) {
      return Integer.compare(order.get(dto1.getStereotype()), order.get(dto2.getStereotype()));
    }
  };

  @Data
  public static class SearchEnvelopeRequestDTO {
    private BoundingBox boundingBox;
    // todo use zoom level, limit allowed values, or something. this is a potential out of memory server killer if abused!
    private Double distanceTolerance;
  }

  @POST
  @Path("nvr/search/envelope")
  public Response search(
      SearchEnvelopeRequestDTO request
  ) {
    try {

      // query rough geometry envelope index for items that match.

      Set<UUID> searchResults = index.search(request.getBoundingBox());
      Set<NaturvardsregistretObject> objects = prevayler.execute(new Query<Root, Set<NaturvardsregistretObject>>() {
        @Override
        public Set<NaturvardsregistretObject> query(Root root, Date date) throws Exception {
          Set<NaturvardsregistretObject> objects = new HashSet<>(searchResults.size());
          for (UUID identity : searchResults) {
            objects.add(root.getNaturvardsregistretObjects().get(identity));
          }
          return objects;
        }
      });

      // filter out non matching items

      Polygon polygon = new GeometryFactory().createPolygon(new Coordinate[]{
          new Coordinate(request.getBoundingBox().getWestLongitude(), request.getBoundingBox().getSouthLatitude()),
          new Coordinate(request.getBoundingBox().getWestLongitude(), request.getBoundingBox().getNorthLatitude()),
          new Coordinate(request.getBoundingBox().getEastLongitude(), request.getBoundingBox().getNorthLatitude()),
          new Coordinate(request.getBoundingBox().getEastLongitude(), request.getBoundingBox().getSouthLatitude()),
          new Coordinate(request.getBoundingBox().getWestLongitude(), request.getBoundingBox().getSouthLatitude()),
      });

      for (Iterator<NaturvardsregistretObject> iterator = objects.iterator(); iterator.hasNext(); ) {
        NaturvardsregistretObject object = iterator.next();
        Geometry geometry = geometryManager.getJtsGeometry(object);
        if (!geometry.intersects(polygon) && !polygon.contains(geometry)) {
          iterator.remove();
        }
      }

      List<SearchResultDTO> searchResultDTOS = new ArrayList<>();
      for (NaturvardsregistretObject object : objects) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setIdentity(object.getIdentity());
        dto.setStereotype(object.getStereotype());
        org.wololo.geojson.Geometry geometry;
        if (request.getDistanceTolerance() != null) {
          geometry = geometryManager.getSimplifiedGeoJsonGeometry(object, request.getDistanceTolerance());
        } else {
          geometry = geometryManager.getGeoJsonGeometry(object);
        }
        dto.setNvrid(object.getNaturvardsregistretIdentity());
        dto.setFeature(new Feature(geometry, Collections.emptyMap()));
        dto.setCentroid(geometryManager.getGeoJsonFeatureCentroid(object));
        dto.setLabel(object.getWikidataLabel());
        for (String filename : object.getWikidataImageNames()) {
          WikimediaImage wikimediaImage = prevayler.execute(new GetWikimediaImage(filename));
          if (wikimediaImage == null) {
            log.warn("Missing Wikimedia image for filename {}", filename);
          } else {
            SearchResultImageDTO image = new SearchResultImageDTO();
            image.setUrl(wikimediaImage.getThumburl());
            image.setFilename(wikimediaImage.getFilename());
            image.setWidth(wikimediaImage.getThumbwidth());
            image.setHeight(wikimediaImage.getThumbheight());
            dto.getImages().add(image);
          }
        }
        dto.setQ(object.getWikidataQ());
        searchResultDTOS.add(dto);
      }

      searchResultDTOS.sort(searchResultDTOComparator);

      return Response.ok().entity(searchResultDTOS).build();

    } catch (Exception e) {
      log.warn("Caught exception", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

}
