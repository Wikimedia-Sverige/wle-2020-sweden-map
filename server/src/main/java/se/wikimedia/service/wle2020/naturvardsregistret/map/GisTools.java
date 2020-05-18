package se.wikimedia.service.wle2020.naturvardsregistret.map;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

public class GisTools {

  public static org.locationtech.jts.geom.Point calculateContainedCentroid(Geometry geometry) {
    GeometryFactory geometryFactory = new GeometryFactory();
    GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(geometryFactory.getPrecisionModel());
    org.locationtech.jts.geom.Point centroid = geometry.getCentroid();
    centroid = (Point) reducer.reduce(centroid);
    geometry = reducer.reduce(geometry);
    if (!geometry.contains(centroid) && !geometry.intersects(centroid)) {
      // find closest vertex
      double closestDistance = Double.MAX_VALUE;
      org.locationtech.jts.geom.Point closestPoint = centroid;
      for (Coordinate coordinate : geometry.getCoordinates()) {
        org.locationtech.jts.geom.Point point = geometryFactory.createPoint(coordinate);
        double distance = centroid.distance(point);
        if (distance < closestDistance) {
          closestDistance = distance;
          closestPoint = point;
        }
      }
      centroid = closestPoint;
    }
    return centroid;
  }


}
