package se.wikimedia.service.wle2020.naturvardsregistret.map.index;

import lombok.Data;

import java.io.Serializable;

@Data
public class BoundingBox implements Serializable {

  public static final long serialVersionUID = 1L;

  public BoundingBox() {
  }

  public BoundingBox(double southLatitude, double westLongitude, double northLatitude, double eastLongitude) {
    this.southLatitude = southLatitude;
    this.westLongitude = westLongitude;
    this.northLatitude = northLatitude;
    this.eastLongitude = eastLongitude;
  }

  private double southLatitude;
  private double westLongitude;
  private double northLatitude;
  private double eastLongitude;

}
