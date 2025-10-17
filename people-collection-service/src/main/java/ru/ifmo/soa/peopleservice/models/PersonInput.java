package ru.ifmo.soa.peopleservice.models;

import ru.ifmo.soa.peopleservice.entities.Coordinates;
import ru.ifmo.soa.peopleservice.entities.Country;
import ru.ifmo.soa.peopleservice.entities.EyeColor;
import ru.ifmo.soa.peopleservice.entities.HairColor;
import ru.ifmo.soa.peopleservice.entities.Location;

import jakarta.validation.constraints.NotNull;

public class PersonInput {
    @NotNull
    private String name;

    @NotNull
    private Coordinates coordinates;

    private Float height;

    @NotNull
    private EyeColor eyeColor;

    private HairColor hairColor;

    private Country nationality;

    @NotNull
    private Location location;

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
