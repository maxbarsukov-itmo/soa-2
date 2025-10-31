package ru.ifmo.soa.peopleservice.dto;

import jakarta.validation.constraints.NotNull;

public class LocationDto {
  @NotNull
  private Integer x;
  @NotNull
  private Long y;
  @NotNull
  private Integer z;
  private String name;

  public LocationDto() {
  }

  public LocationDto(Integer x, Long y, Integer z, String name) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.name = name;
  }

  public Integer getX() {
    return x;
  }

  public void setX(Integer x) {
    this.x = x;
  }

  public Long getY() {
    return y;
  }

  public void setY(Long y) {
    this.y = y;
  }

  public Integer getZ() {
    return z;
  }

  public void setZ(Integer z) {
    this.z = z;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
