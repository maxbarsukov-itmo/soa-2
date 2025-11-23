package ru.ifmo.soa.peopleservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;

@XmlRootElement(name = "PersonInput")
@XmlAccessorType(XmlAccessType.FIELD)
public class PersonInputDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @NotNull private String name;
  @NotNull private CoordinatesDto coordinates;
  private Float height;
  @NotNull private String eyeColor;
  private String hairColor;
  private String nationality;
  @NotNull private LocationDto location;

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
