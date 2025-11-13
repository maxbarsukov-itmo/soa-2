package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.ejb.remote.DeletePersonByLocationRemote;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.exceptions.NotFoundException;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

@Stateless
@Transactional
public class DeletePersonByLocationBean implements DeletePersonByLocationRemote {

  @Inject private PersonRepository repository;

  @Override
  public Result<Void> deletePersonByLocation(Location location) {
    try {
      int deleted = repository.deleteByLocation(location);
      if (deleted == 0) {
        throw new NotFoundException("No person found with the specified location");
      }

      return new Result.Success<>(null);
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
