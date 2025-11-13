package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.dto.PersonInputDto;
import ru.ifmo.soa.peopleservice.ejb.remote.AddPersonRemote;
import ru.ifmo.soa.peopleservice.entities.Country;
import ru.ifmo.soa.peopleservice.entities.EyeColor;
import ru.ifmo.soa.peopleservice.entities.HairColor;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

import java.time.OffsetDateTime;

@Stateless
@Transactional
public class AddPersonBean implements AddPersonRemote {

  @Inject private PersonRepository repository;
  @Inject private PersonMapper mapper;

  @Override
  public Result<PersonDto> addPerson(PersonInputDto personInput) {
    try {
      validatePersonInput(personInput);
      if (repository.isStorageFull()) {
        throw new InsufficientStorageException("Server storage capacity exceeded");
      }
      Person entity = mapper.toEntity(personInput);
      if (repository.existsSimilarPerson(entity)) {
        throw new ConflictException("A person with these attributes already exists in the collection");
      }
      entity.setCreationDate(OffsetDateTime.now());
      repository.save(entity);
      return new Result.Success<>(mapper.toDto(entity));
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }

  private void validatePersonInput(PersonInputDto input) {
    if (input.getHeight() != null && input.getHeight() <= 0) {
      throw new SemanticException("Height must be greater than 0");
    }
    if (input.getName() == null || input.getName().trim().isEmpty()) {
      throw new SemanticException("Name is required and cannot be empty");
    }
    if (input.getCoordinates() == null) {
      throw new SemanticException("Coordinates are required");
    }
    if (input.getCoordinates().getX() == null) {
      throw new SemanticException("Coordinate x is required");
    }
    if (input.getCoordinates().getY() == null) {
      throw new SemanticException("Coordinate y is required");
    }
    if (input.getEyeColor() == null) {
      throw new SemanticException("Eye color is required");
    } else {
      try {
        EyeColor.valueOf(input.getEyeColor());
      } catch (IllegalArgumentException e2) {
        throw new SemanticException("Invalid eye color value: " + input.getEyeColor());
      }
    }
    if (input.getNationality() != null) {
      try {
        Country.valueOf(input.getNationality());
      } catch (IllegalArgumentException e2) {
        throw new SemanticException("Invalid nationality value: " + input.getNationality());
      }
    }
    if (input.getHairColor() != null) {
      try {
        HairColor.valueOf(input.getHairColor());
      } catch (IllegalArgumentException e2) {
        throw new SemanticException("Invalid hair color value: " + input.getHairColor());
      }
    }
    if (input.getLocation() == null) {
      throw new SemanticException("Location is required");
    }
    if (input.getLocation().getX() == null) {
      throw new SemanticException("Location x is required");
    }
    if (input.getLocation().getY() == null) {
      throw new SemanticException("Location y is required");
    }
    if (input.getLocation().getZ() == null) {
      throw new SemanticException("Location z is required");
    }
    if (input.getLocation().getName() != null && input.getLocation().getName().length() > 704) {
      throw new SemanticException("Location name cannot exceed 704 characters");
    }
  }
}
