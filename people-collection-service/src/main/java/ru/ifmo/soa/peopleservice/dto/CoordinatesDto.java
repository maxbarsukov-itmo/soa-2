package ru.ifmo.soa.peopleservice.dto;

import jakarta.validation.constraints.NotNull;

public class CoordinatesDto {
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
