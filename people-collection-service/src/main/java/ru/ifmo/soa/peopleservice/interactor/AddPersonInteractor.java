package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.dto.PersonInputDto;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

import java.time.OffsetDateTime;

@ApplicationScoped
public class AddPersonInteractor {

  @Inject
  private PersonRepository repository;

  @Inject
  private PersonMapper mapper;

  @Transactional
  public Response execute(PersonInputDto personInput) {
    try {
      validatePersonInput(personInput);
      if (repository.isStorageFull()) {
        throw new InsufficientStorageException("Server storage capacity exceeded");
      }
      if (repository.existsSimilarPerson(mapper.toEntity(personInput))) {
        throw new ConflictException("A person with these attributes already exists in the collection");
      }
      Person person = mapper.toEntity(personInput);
      person.setCreationDate(OffsetDateTime.now());
      repository.save(person);
      PersonDto responseDto = mapper.toDto(person);
      return Response.status(Response.Status.CREATED).entity(responseDto).build();
    } catch (IllegalArgumentException e) {
      throw new SemanticException("Request body contains malformed JSON or invalid data format");
    } catch (ConflictException e) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorResponseDto(409, e.getMessage())).build();
    } catch (ContentTooLargeException e) {
      return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).entity(new ErrorResponseDto(413, e.getMessage())).build();
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponseDto(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponseDto(422, e.getMessage())).build();
    } catch (InsufficientStorageException e) {
      return Response.status(507, "Insufficient Storage").entity(new ErrorResponseDto(507, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, e.getMessage())).build();
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
