package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

@ApplicationScoped
public class GetPersonByIdInteractor {

  @Inject
  private PersonRepository repository;

  @Inject
  private PersonMapper mapper;

  @Transactional
  public Response execute(Long id) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Provided ID parameter is invalid");
      }
      Person person = repository.findById(id);
      PersonDto responseDto = mapper.toDto(person);
      return Response.ok(responseDto).build();
    } catch (BadRequestException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseDto(400, e.getMessage())).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponseDto(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred while retrieving the person")).build();
    }
  }
}
