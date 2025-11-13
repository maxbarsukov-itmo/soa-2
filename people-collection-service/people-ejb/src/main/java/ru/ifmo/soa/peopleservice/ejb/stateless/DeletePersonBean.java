package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.ejb.remote.DeletePersonRemote;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

@Stateless
@Transactional
public class DeletePersonBean implements DeletePersonRemote {

  @Inject private PersonRepository repository;

  @Override
  public Result<Void> deletePerson(Long id) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Provided ID parameter is invalid");
      }
      var person = repository.findById(id);
      repository.delete(person);

      return new Result.Success<>(null);
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
