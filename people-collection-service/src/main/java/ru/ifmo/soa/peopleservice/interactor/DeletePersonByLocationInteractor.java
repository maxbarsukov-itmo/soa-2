package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

@ApplicationScoped
public class DeletePersonByLocationInteractor {

  @Inject
  private PersonRepository repository;

  @Transactional
  public Response execute(Location location) {
    try {
      int deletedCount = repository.deleteByLocation(location);
      if (deletedCount == 0) {
        throw new NotFoundException("No person found with the specified location");
      }
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Location data in request body is invalid or malformed");
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponseDto(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponseDto(422, e.getMessage())).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponseDto(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred during location-based deletion")).build();
    }
  }
}
