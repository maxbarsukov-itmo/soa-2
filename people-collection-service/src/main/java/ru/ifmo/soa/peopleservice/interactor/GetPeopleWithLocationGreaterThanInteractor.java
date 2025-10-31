package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;

import java.util.List;

@ApplicationScoped
public class GetPeopleWithLocationGreaterThanInteractor {

  @Inject
  private PersonRepository repository;

  @Inject
  private PersonMapper mapper;

  @Transactional
  public Response execute(Integer x, Long y, Integer z) {
    try {
      if (x == null) {
        throw new BadRequestException("Parameter 'x' is required");
      }
      if (y == null) {
        throw new BadRequestException("Parameter 'y' is required");
      }
      if (z == null) {
        throw new BadRequestException("Parameter 'z' is required");
      }
      List<Person> people = repository.findWithLocationGreaterThan(x, y, z);
      long totalCount = people.size();
      int totalPages = (int) Math.ceil((double) totalCount / 10.0);
      List<PersonDto> personDtos = mapper.toDtoList(people);
      PeopleResponseDto response = new PeopleResponseDto(personDtos, 0, 10, totalPages, totalCount);
      return Response.ok(response).build();
    } catch (BadRequestException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponseDto(400, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponseDto(422, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred during location comparison")).build();
    }
  }
}
