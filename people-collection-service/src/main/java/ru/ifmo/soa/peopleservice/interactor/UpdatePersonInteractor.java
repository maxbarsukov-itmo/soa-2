package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.entities.Coordinates;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

import java.lang.reflect.Field;
import java.util.Map;

@ApplicationScoped
public class UpdatePersonInteractor {

  @Inject
  private PersonRepository repository;

  @Inject
  private PersonMapper mapper;

  @Transactional
  public Response execute(Long id, Map<String, Object> updates) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Invalid ID parameter");
      }
      if (updates == null || updates.isEmpty()) {
        throw new BadRequestException("Update payload cannot be empty");
      }
      Person existingPerson = repository.findById(id);
      applyUpdates(existingPerson, updates);
      repository.update(existingPerson);
      PersonDto responseDto = mapper.toDto(existingPerson);
      return Response.ok(responseDto).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid ID parameter or request body");
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponseDto(404, e.getMessage())).build();
    } catch (MethodNotAllowedException e) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(new ErrorResponseDto(405, e.getMessage())).build();
    } catch (ConflictException e) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorResponseDto(409, e.getMessage())).build();
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponseDto(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponseDto(422, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred while updating the person")).build();
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
      if ("name".equals(fieldName)) {
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
          throw new SemanticException("Name cannot be empty");
        }
        person.setName((String) value);
      } else if ("height".equals(fieldName)) {
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
      } else if ("eyeColor".equals(fieldName)) {
        try {
          person.setEyeColor(ru.ifmo.soa.peopleservice.entities.EyeColor.valueOf((String) value));
        } catch (IllegalArgumentException e2) {
          throw new SemanticException("Invalid eye color value: " + value);
        }
      } else if ("hairColor".equals(fieldName)) {
        if (value == null) {
          person.setHairColor(null);
        } else {
          try {
            person.setHairColor(ru.ifmo.soa.peopleservice.entities.HairColor.valueOf((String) value));
          } catch (IllegalArgumentException e2) {
            throw new SemanticException("Invalid hair color value: " + value);
          }
        }
      } else if ("nationality".equals(fieldName)) {
        if (value == null) {
          person.setNationality(null);
        } else {
          try {
            person.setNationality(ru.ifmo.soa.peopleservice.entities.Country.valueOf((String) value));
          } catch (IllegalArgumentException e2) {
            throw new SemanticException("Invalid nationality value: " + value);
          }
        }
      } else if ("coordinates".equals(fieldName)) {
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
      } else if ("location".equals(fieldName)) {
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
      } else {
        throw new SemanticException("Unknown field: " + fieldName);
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to update field: " + fieldName, e);
    }
  }
}
