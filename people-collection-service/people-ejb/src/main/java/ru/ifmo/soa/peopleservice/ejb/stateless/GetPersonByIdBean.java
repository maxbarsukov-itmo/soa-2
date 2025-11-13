package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.ejb.remote.GetPersonByIdRemote;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

@Stateless
@Transactional
public class GetPersonByIdBean implements GetPersonByIdRemote {

  @Inject private PersonRepository repository;
  @Inject private PersonMapper mapper;

  @Override
  public Result<PersonDto> getPersonById(Long id) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Provided ID parameter is invalid");
      }
      var person = repository.findById(id);

      return new Result.Success<>(mapper.toDto(person));
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
