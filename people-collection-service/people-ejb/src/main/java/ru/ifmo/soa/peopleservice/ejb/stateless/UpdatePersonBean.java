package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.ejb.remote.UpdatePersonRemote;
import ru.ifmo.soa.peopleservice.entities.*;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

import java.lang.reflect.Field;
import java.util.Map;

@Stateless
@Transactional
public class UpdatePersonBean implements UpdatePersonRemote {

  @Inject private PersonRepository repository;
  @Inject private PersonMapper mapper;

  @Override
  public Result<PersonDto> updatePerson(Long id, Map<String, Object> updates) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Invalid ID parameter");
      }
      if (updates == null || updates.isEmpty()) {
        throw new BadRequestException("Update payload cannot be empty");
      }
      Person person = repository.findById(id);
      applyUpdates(person, updates);
      repository.update(person);
      return new Result.Success<>(mapper.toDto(person));
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }

  private void applyUpdates(Person person, Map<String, Object> updates) {
    for (Map.Entry<String, Object> entry : updates.entrySet()) {
      String fieldName = entry.getKey();
      Object value = entry.getValue();
      setField(person, fieldName, value);
    }
  }

  private void setField(Person person, String fieldName, Object value) {
    try {
      Field field = Person.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      if (value == null) {
        field.set(person, null);
        return;
      }

      Class<?> fieldType = field.getType();
      if (fieldType.isEnum()) {
        field.set(person, Enum.valueOf((Class<Enum>) fieldType, value.toString()));
      } else if (fieldType == Coordinates.class && value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> coordsMap = (Map<String, Object>) value;
        Coordinates currentCoords = person.getCoordinates();
        Coordinates newCoords = new Coordinates(
          (Integer) coordsMap.getOrDefault("x", currentCoords.getX()),
          (Integer) coordsMap.getOrDefault("y", currentCoords.getY())
        );
        person.setCoordinates(newCoords);
      } else if (fieldType == Location.class && value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> locationMap = (Map<String, Object>) value;
        Location currentLocation = person.getLocation();
        Location newLocation = new Location(
          (Integer) locationMap.getOrDefault("x", currentLocation.getX()),
          ((Number) locationMap.getOrDefault("y", currentLocation.getY())).longValue(),
          (Integer) locationMap.getOrDefault("z", currentLocation.getZ()),
          (String) locationMap.getOrDefault("name", currentLocation.getName())
        );
        if (newLocation.getName() != null && newLocation.getName().length() > 704) {
          throw new SemanticException("Location name cannot exceed 704 characters");
        }
        person.setLocation(newLocation);
      } else if (fieldType == Float.class || fieldType == float.class) {
        if (value instanceof Number) {
          float height = ((Number) value).floatValue();
          if (height <= 0) {
            throw new SemanticException("Height must be greater than 0");
          }
          field.set(person, height);
        } else {
          throw new SemanticException(fieldName + " must be a number");
        }
      } else if (fieldType == String.class) {
        String strValue = (String) value;
        if ("name".equals(fieldName) && strValue.trim().isEmpty()) {
          throw new SemanticException("Name cannot be empty");
        }
        if ("location.name".equals(fieldName) && strValue.length() > 704) {
          throw new SemanticException("Location name cannot exceed 704 characters");
        }
        field.set(person, strValue);
      } else {
        field.set(person, value);
      }
    } catch (NoSuchFieldException e) {
      switch (fieldName) {
        case "name" -> {
          if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new SemanticException("Name cannot be empty");
          }
          person.setName((String) value);
        }
        case "height" -> {
          if (value == null) {
            person.setHeight(null);
          } else if (value instanceof Number) {
            float height = ((Number) value).floatValue();
            if (height <= 0) {
              throw new SemanticException("Height must be greater than 0");
            }
            person.setHeight(height);
          } else {
            throw new SemanticException("Height must be a number");
          }
        }
        case "eyeColor" -> {
          try {
            person.setEyeColor(EyeColor.valueOf((String) value));
          } catch (IllegalArgumentException e2) {
            throw new SemanticException("Invalid eye color value: " + value);
          }
        }
        case "hairColor" -> {
          if (value == null) {
            person.setHairColor(null);
          } else {
            try {
              person.setHairColor(HairColor.valueOf((String) value));
            } catch (IllegalArgumentException e2) {
              throw new SemanticException("Invalid hair color value: " + value);
            }
          }
        }
        case "nationality" -> {
          if (value == null) {
            person.setNationality(null);
          } else {
            try {
              person.setNationality(Country.valueOf((String) value));
            } catch (IllegalArgumentException e2) {
              throw new SemanticException("Invalid nationality value: " + value);
            }
          }
        }
        case "coordinates" -> {
          if (!(value instanceof Map)) {
            throw new SemanticException("Coordinates must be an object");
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> coordsMap = (Map<String, Object>) value;
          Coordinates currentCoords = person.getCoordinates();
          Coordinates newCoords = new Coordinates(
            (Integer) coordsMap.getOrDefault("x", currentCoords.getX()),
            (Integer) coordsMap.getOrDefault("y", currentCoords.getY())
          );
          person.setCoordinates(newCoords);
        }
        case "location" -> {
          if (!(value instanceof Map)) {
            throw new SemanticException("Location must be an object");
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> locationMap = (Map<String, Object>) value;
          Location currentLocation = person.getLocation();
          Location newLocation = new Location(
            (Integer) locationMap.getOrDefault("x", currentLocation.getX()),
            ((Number) locationMap.getOrDefault("y", currentLocation.getY())).longValue(),
            (Integer) locationMap.getOrDefault("z", currentLocation.getZ()),
            (String) locationMap.getOrDefault("name", currentLocation.getName())
          );
          if (newLocation.getName() != null && newLocation.getName().length() > 704) {
            throw new SemanticException("Location name cannot exceed 704 characters");
          }
          person.setLocation(newLocation);
        }
        default -> throw new SemanticException("Unknown field: " + fieldName);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to update field: " + fieldName, e);
    }
  }
}
