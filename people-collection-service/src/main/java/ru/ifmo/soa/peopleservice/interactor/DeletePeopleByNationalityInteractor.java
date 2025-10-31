package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.entities.Country;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

@ApplicationScoped
public class DeletePeopleByNationalityInteractor {

  @Inject
  private PersonRepository repository;

  @Transactional
  public Response execute(String nationality) {
    try {
      Country country = Country.valueOf(nationality);
      int deletedCount = repository.deleteByNationality(country);
      if (deletedCount == 0) {
        throw new NotFoundException("No people found with the specified nationality");
      }
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Provided nationality parameter is invalid");
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponseDto(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred during bulk deletion")).build();
    }
  }
}
