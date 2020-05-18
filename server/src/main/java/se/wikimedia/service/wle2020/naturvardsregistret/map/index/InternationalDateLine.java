package se.wikimedia.service.wle2020.naturvardsregistret.map.index;

/**
 * @author kalle
 * @since 2015-10-28 10:46
 */
public class InternationalDateLine {

  public boolean spans(BoundingBox boundingBox) {
    return boundingBox.getWestLongitude() > boundingBox.getEastLongitude();
  }

  public BoundingBox[] split(BoundingBox boundingBox) {
    if (!spans(boundingBox)) {
      return new BoundingBox[]{boundingBox};
    } else {
      return new BoundingBox[]{
          new BoundingBox(boundingBox.getSouthLatitude(), boundingBox.getWestLongitude(), boundingBox.getNorthLatitude(), 180d),
          new BoundingBox(boundingBox.getSouthLatitude(), -180d, boundingBox.getNorthLatitude(), boundingBox.getEastLongitude()),
      };
    }
  }

}
