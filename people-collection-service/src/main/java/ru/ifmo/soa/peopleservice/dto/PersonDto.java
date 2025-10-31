package ru.ifmo.soa.peopleservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class PersonDto {
  private Long id;
  @NotNull
  private String name;
  @NotNull
  private CoordinatesDto coordinates;
  private OffsetDateTime creationDate;
  private Float height;
  @NotNull
  private String eyeColor;
  private String hairColor;
  private String nationality;
  @NotNull
  private LocationDto location;

  public PersonDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CoordinatesDto getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(CoordinatesDto coordinates) {
    this.coordinates = coordinates;
  }

  public OffsetDateTime getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(OffsetDateTime creationDate) {
    this.creationDate = creationDate;
  }

  public Float getHeight() {
    return height;
  }

  public void setHeight(Float height) {
    this.height = height;
  }

  public String getEyeColor() {
    return eyeColor;
  }

  public void setEyeColor(String eyeColor) {
    this.eyeColor = eyeColor;
  }

  public String getHairColor() {
    return hairColor;
  }

  public void setHairColor(String hairColor) {
    this.hairColor = hairColor;
  }

  public String getNationality() {
    return nationality;
  }

  public void setNationality(String nationality) {
    this.nationality = nationality;
  }

  public LocationDto getLocation() {
    return location;
  }

  public void setLocation(LocationDto location) {
    this.location = location;
  }
}
