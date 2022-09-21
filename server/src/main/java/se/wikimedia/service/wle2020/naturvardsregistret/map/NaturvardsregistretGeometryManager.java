package se.wikimedia.service.wle2020.naturvardsregistret.map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.prevayler.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wololo.jts2geojson.GeoJSONReader;
import org.wololo.jts2geojson.GeoJSONWriter;
import se.wikimedia.service.template.Initializable;
import se.wikimedia.service.template.prevayler.PrevaylerManager;
import se.wikimedia.service.template.prevayler.Root;
import se.wikimedia.service.wle2020.naturvardsregistret.map.domain.NaturvardsregistretObject;

import java.util.*;

@Singleton
public class NaturvardsregistretGeometryManager implements Initializable {

  private Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  private ObjectMapper objectMapper;

  @Inject
  private PrevaylerManager prevayler;

  @Override
  public boolean open() throws Exception {

    // pre calculate all geometries
    // and simplified geometries

    // this needs to match the data in index.js
    double[] distanceTolerances = new double[]{
      0.1,
      0.05,
      0.025,
      0.005,
      0.002,
      0.001,
      0.0005,
      0.0001
    };

    for (NaturvardsregistretObject object : prevayler.execute(new Query<Root, Collection<NaturvardsregistretObject>>() {
      @Override
      public Collection<NaturvardsregistretObject> query(Root root, Date date) throws Exception {
        return new HashSet<>(root.getNaturvardsregistretObjects().values());
      }
    })) {
      if (object.getFeatureGeometry() != null) {
        getJtsGeometry(object);
        getGeoJsonGeometry(object);
        getGeoJsonFeatureCentroid(object);
        for (double distanceTolerance : distanceTolerances) {
          getSimplifiedGeoJsonGeometry(object, distanceTolerance);
        }
      } else {
        log.warn("{} is missing feature geometry", object.getWikidataQ());
      }
    }

    log.info("{} jts geometries, {} geojson geometries, {} simplified geometries", jtsGeometries.size(), geoJsonGeometries.size(), simplifiedGeometries.size());

    return true;
  }

  @Override
  public boolean close() throws Exception {
    return true;
  }

  private final LoadingCache<NaturvardsregistretObject, Geometry> jtsGeometries = CacheBuilder
          .newBuilder()
          .maximumSize(100)
          .build(new CacheLoader<NaturvardsregistretObject, Geometry>() {
    @Override
    public Geometry load(NaturvardsregistretObject object) {
      return new GeoJSONReader().read(object.getFeatureGeometry());
    }
  });

  public Geometry getJtsGeometry(NaturvardsregistretObject object) throws Exception {
    return jtsGeometries.get(object);
  }

  private LoadingCache<NaturvardsregistretObject, org.wololo.geojson.Geometry> geoJsonGeometries = CacheBuilder
          .newBuilder()
          .maximumSize(100)
          .build(new CacheLoader<NaturvardsregistretObject, org.wololo.geojson.Geometry>() {
    @Override
    public org.wololo.geojson.Geometry load(NaturvardsregistretObject object) throws Exception {
      return objectMapper.readValue(object.getFeatureGeometry(), org.wololo.geojson.Geometry.class);
    }
  });

  public org.wololo.geojson.Geometry getGeoJsonGeometry(NaturvardsregistretObject object) throws Exception {
    return geoJsonGeometries.get(object);
  }

  private Map<UUID, org.wololo.geojson.Point> geoJsonCentroids = new HashMap<>(10000);

  public org.wololo.geojson.Point getGeoJsonFeatureCentroid(NaturvardsregistretObject object) throws Exception {
    org.wololo.geojson.Point centroid = geoJsonCentroids.get(object.getIdentity());
    if (centroid == null) {
      centroid = (org.wololo.geojson.Point)geoJSONWriter.write(GisTools.calculateContainedCentroid(getJtsGeometry(object)));
      geoJsonCentroids.put(object.getIdentity(), centroid);
    }
    return centroid;
  }

  @Data
  private static class SimplifiedGeoJsonGeometryKey {
    private NaturvardsregistretObject object;
    private double distanceTolerance;

    public SimplifiedGeoJsonGeometryKey(NaturvardsregistretObject object, double distanceTolerance) {
      this.object = object;
      this.distanceTolerance = distanceTolerance;
    }
  }

  private GeometryFactory geometryFactory = new GeometryFactory();
  private GeoJSONWriter geoJSONWriter = new GeoJSONWriter();

  private LoadingCache<SimplifiedGeoJsonGeometryKey, org.wololo.geojson.Geometry> simplifiedGeometries = CacheBuilder
          .newBuilder()
          .maximumSize(100)
          .build(new CacheLoader<SimplifiedGeoJsonGeometryKey, org.wololo.geojson.Geometry>() {
            @Override
            public org.wololo.geojson.Geometry load(SimplifiedGeoJsonGeometryKey simplifiedGeoJsonGeometryKey) throws Exception {
              Geometry jtsGeometry = getJtsGeometry(simplifiedGeoJsonGeometryKey.getObject());
              if (jtsGeometry instanceof Polygon
                      || jtsGeometry instanceof MultiPolygon) {
                Geometry simplifiedJtsGeometry = TopologyPreservingSimplifier.simplify(jtsGeometry, simplifiedGeoJsonGeometryKey.getDistanceTolerance());
                return geoJSONWriter.write(simplifiedJtsGeometry);

              } else if (jtsGeometry instanceof MultiPoint) {
                Coordinate[] coordinates = jtsGeometry.getCoordinates();
                if (coordinates.length == 1) {
                  return geoJSONWriter.write(geometryFactory.createPoint(coordinates[0]));
                } else if (coordinates.length == 2) {
                  return geoJSONWriter.write(jtsGeometry.getCentroid());
                } else {
                  Geometry convexHull = jtsGeometry.convexHull();
                  Geometry simplifiedConvexHull = TopologyPreservingSimplifier.simplify(convexHull, simplifiedGeoJsonGeometryKey.getDistanceTolerance());
                  return geoJSONWriter.write(simplifiedConvexHull);
                }
              }
              return getGeoJsonGeometry(simplifiedGeoJsonGeometryKey.getObject());
            }
          });

  /**
   * @param object
   * @param distanceTolerance 0.01 is almost all rectangles. 0.0025 is still very similar to original but often 1/10 of the points.
   * @return
   * @throws Exception
   */
  public org.wololo.geojson.Geometry getSimplifiedGeoJsonGeometry(NaturvardsregistretObject object, double distanceTolerance) throws Exception {
    return simplifiedGeometries.get(new SimplifiedGeoJsonGeometryKey(object, distanceTolerance));
  }

}
