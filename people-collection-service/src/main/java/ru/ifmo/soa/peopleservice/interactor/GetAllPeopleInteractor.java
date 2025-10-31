package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.PathResolver;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class GetAllPeopleInteractor {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "id", "name", "creationDate", "coordinates.x", "coordinates.y",
    "height", "eyeColor", "hairColor", "nationality",
    "location.x", "location.y", "location.z", "location.name"
  );
  @Inject
  private PersonRepository repository;
  @Inject
  private PersonMapper mapper;

  @Transactional
  public Response execute(String sortBy, String sortOrder, Integer page, Integer pageSize) {
    if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
      throw new BadRequestException("Invalid sortBy field: " + sortBy);
    }
    if (pageSize < 0) {
      throw new SemanticException("Page size cannot be negative");
    }
    if (page < 0) {
      throw new SemanticException("Page number cannot be negative");
    }
    try {
      PathResolver.SortInfo sortInfo = new PathResolver.SortInfo(sortBy, sortOrder);
      List<Person> people = repository.findAll(page, pageSize, sortInfo);
      long totalCount = repository.countAll();
      int totalPages = (int) Math.ceil((double) totalCount / pageSize);
      List<PersonDto> personDtos = mapper.toDtoList(people);
      PeopleResponseDto response = new PeopleResponseDto(personDtos, page, pageSize, totalPages, totalCount);
      return Response.ok(response).build();
    } catch (NumberFormatException e) {
      throw new BadRequestException("One or more query parameters contain invalid values");
    } catch (Exception e) {
      if (e instanceof SemanticException) {
        throw e;
      }
      throw new InternalServerErrorException("An unexpected error occurred while fetching people");
    }
  }
}
