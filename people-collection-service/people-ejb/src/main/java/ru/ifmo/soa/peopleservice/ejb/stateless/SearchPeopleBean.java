package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.FilterCriteriaDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.ejb.remote.SearchPeopleRemote;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.exceptions.SemanticException;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.PathResolver;
import ru.ifmo.soa.peopleservice.util.Result;

import java.util.List;
import java.util.Set;

@Stateless
@Transactional
public class SearchPeopleBean implements SearchPeopleRemote {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "id", "name", "creationDate", "coordinates.x", "coordinates.y",
    "height", "eyeColor", "hairColor", "nationality",
    "location.x", "location.y", "location.z", "location.name"
  );

  @Inject private PersonRepository repository;
  @Inject private PersonMapper mapper;

  @Override
  public Result<PeopleResponseDto> searchPeople(FilterCriteriaDto filterCriteria, String sortBy, String sortOrder, Integer page, Integer pageSize) {
    try {
      if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
        throw new BadRequestException("Invalid sortBy field: " + sortBy);
      }
      if (pageSize < 0) throw new SemanticException("Page size cannot be negative");
      if (page < 0) throw new SemanticException("Page number cannot be negative");

      PathResolver.SortInfo sortInfo = new PathResolver.SortInfo(sortBy, sortOrder);
      List<Person> people = repository.findWithFilters(filterCriteria, page, pageSize, sortInfo);
      long totalCount = repository.countWithFilters(filterCriteria);
      int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
      List<PersonDto> dtos = mapper.toDtoList(people);
      return new Result.Success<>(new PeopleResponseDto(dtos, page, pageSize, totalPages, totalCount));
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
