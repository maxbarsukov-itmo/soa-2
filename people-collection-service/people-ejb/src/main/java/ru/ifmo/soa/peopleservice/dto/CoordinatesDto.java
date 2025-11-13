package ru.ifmo.soa.peopleservice.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class CoordinatesDto implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @NotNull
  private Integer x;
  @NotNull
  private Integer y;

  public CoordinatesDto() {
  }

  public CoordinatesDto(Integer x, Integer y) {
    this.x = x;
    this.y = y;
  }

  public Integer getX() {
    return x;
  }

  public void setX(Integer x) {
    this.x = x;
  }

  public Integer getY() {
    return y;
  }

  public void setY(Integer y) {
    this.y = y;
  }
}
