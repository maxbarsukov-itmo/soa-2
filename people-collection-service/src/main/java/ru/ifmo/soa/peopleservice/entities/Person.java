package ru.ifmo.soa.peopleservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "people")
public class Person {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  private String name;

  @Embedded
  private Coordinates coordinates;

  @Column(name = "creation_date", nullable = false, updatable = false)
  private LocalDateTime creationDate;

  private Float height;

  @Enumerated(EnumType.STRING)
  private EyeColor eyeColor;

  @Enumerated(EnumType.STRING)
  private HairColor hairColor;

  @Enumerated(EnumType.STRING)
  private Country nationality;

  @Embedded
  private Location location;

  public Person() {}

  public Person(String name, Coordinates coordinates, Float height, EyeColor eyeColor, HairColor hairColor, Country nationality, Location location) {
    this.name = name;
    this.coordinates = coordinates;
    this.height = height;
    this.eyeColor = eyeColor;
    this.hairColor = hairColor;
    this.nationality = nationality;
    this.location = location;
    this.creationDate = LocalDateTime.now();
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

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public void setCoordinates(Coordinates coordinates) {
    this.coordinates = coordinates;
  }

  public LocalDateTime getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(LocalDateTime creationDate) {
    this.creationDate = creationDate;
  }

  public Float getHeight() {
    return height;
  }

  public void setHeight(Float height) {
    this.height = height;
  }

  public EyeColor getEyeColor() {
    return eyeColor;
  }

  public void setEyeColor(EyeColor eyeColor) {
    this.eyeColor = eyeColor;
  }

  public HairColor getHairColor() {
    return hairColor;
  }

  public void setHairColor(HairColor hairColor) {
    this.hairColor = hairColor;
  }

  public Country getNationality() {
    return nationality;
  }

  public void setNationality(Country nationality) {
    this.nationality = nationality;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }
}
