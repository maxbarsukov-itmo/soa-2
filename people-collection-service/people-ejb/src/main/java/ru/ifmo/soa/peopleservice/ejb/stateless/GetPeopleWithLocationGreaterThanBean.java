package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.ejb.remote.GetPeopleWithLocationGreaterThanRemote;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

import java.util.List;

@Stateless
@Transactional
public class GetPeopleWithLocationGreaterThanBean implements GetPeopleWithLocationGreaterThanRemote {

  @Inject private PersonRepository repository;
  @Inject private PersonMapper mapper;

  @Override
  public Result<PeopleResponseDto> getPeopleWithLocationGreaterThan(Integer x, Long y, Integer z) {
    try {
      if (x == null) throw new BadRequestException("Parameter 'x' is required");
      if (y == null) throw new BadRequestException("Parameter 'y' is required");
      if (z == null) throw new BadRequestException("Parameter 'z' is required");

      List<Person> people = repository.findWithLocationGreaterThan(x, y, z);
      long totalCount = people.size();
      int totalPages = (int) Math.ceil((double) totalCount / 10.0);
      List<PersonDto> dtos = mapper.toDtoList(people);

      return new Result.Success<>(new PeopleResponseDto(dtos, 0, 10, totalPages, totalCount));
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
