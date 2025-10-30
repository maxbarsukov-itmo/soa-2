package ru.ifmo.soa.demographyservice.model;

public enum DemographyField {
  HAIR_COLOR("hairColor"),
  EYE_COLOR("eyeColor");

  public final String apiName;

  DemographyField(String apiName) {
    this.apiName = apiName;
  }
}
