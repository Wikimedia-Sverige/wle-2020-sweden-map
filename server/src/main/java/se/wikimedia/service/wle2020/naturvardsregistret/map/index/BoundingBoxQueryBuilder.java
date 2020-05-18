package se.wikimedia.service.wle2020.naturvardsregistret.map.index;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * @author kalle
 * @since 2015-10-28 10:42
 */
public class BoundingBoxQueryBuilder {

  private BoundingBox boundingBox;

  public BoundingBoxQueryBuilder() {
  }

  public BoundingBoxQueryBuilder(BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public Query build() {

    BoundingBox[] parts = new InternationalDateLine().split(boundingBox);
    BooleanQuery.Builder partQueriesBuilder = new BooleanQuery.Builder();
    for (BoundingBox part : parts) {
      partQueriesBuilder.add(new BooleanClause(build(part), BooleanClause.Occur.SHOULD));
    }
    return partQueriesBuilder.build();

  }

  public Query build(BoundingBox boundingBox) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new BooleanClause(buildIntersectsIndexBoundingBox(boundingBox), BooleanClause.Occur.SHOULD));
    // not required, will also be matched by intersection
//    builder.add(new BooleanClause(buildCoversIndexBoundingBox(boundingBox), BooleanClause.Occur.SHOULD));
    builder.add(new BooleanClause(buildIndexBoundingBoxCovers(boundingBox), BooleanClause.Occur.SHOULD));
    return builder.build();
  }

  public Query buildIntersectsIndexBoundingBox(BoundingBox boundingBox) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_SOUTH_LATITUDE, -90d, boundingBox.getNorthLatitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_WEST_LONGITUDE, -180d, boundingBox.getEastLongitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_NORTH_LATITUDE, boundingBox.getSouthLatitude(), 90d), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_EAST_LONGITUDE, boundingBox.getWestLongitude(), 180d), BooleanClause.Occur.MUST));
    return builder.build();
  }

  public Query buildCoversIndexBoundingBox(BoundingBox boundingBox) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_SOUTH_LATITUDE, boundingBox.getSouthLatitude(), boundingBox.getNorthLatitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_WEST_LONGITUDE, boundingBox.getWestLongitude(), boundingBox.getEastLongitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_NORTH_LATITUDE, boundingBox.getSouthLatitude(), boundingBox.getNorthLatitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_EAST_LONGITUDE, boundingBox.getWestLongitude(), boundingBox.getEastLongitude()), BooleanClause.Occur.MUST));
    return builder.build();
  }

  public Query buildIndexBoundingBoxCovers(BoundingBox boundingBox) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_SOUTH_LATITUDE, -90d, boundingBox.getSouthLatitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_WEST_LONGITUDE, -180d, boundingBox.getWestLongitude()), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_NORTH_LATITUDE, boundingBox.getNorthLatitude(), 90d), BooleanClause.Occur.MUST));
    builder.add(new BooleanClause(DoublePoint.newRangeQuery(NaturvardsregistretIndex.FIELD_EAST_LONGITUDE, boundingBox.getEastLongitude(), 180d), BooleanClause.Occur.MUST));
    return builder.build();
  }

}
