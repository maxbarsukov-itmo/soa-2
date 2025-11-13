package ru.ifmo.soa.peopleservice.ejb.stateless;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.soa.peopleservice.ejb.remote.DeletePeopleByNationalityRemote;
import ru.ifmo.soa.peopleservice.entities.Country;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.exceptions.NotFoundException;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.util.Result;

@Stateless
@Transactional
public class DeletePeopleByNationalityBean implements DeletePeopleByNationalityRemote {

  @Inject private PersonRepository repository;

  @Override
  public Result<Void> deletePeopleByNationality(String nationality) {
    try {
      try {
        Country country = Country.valueOf(nationality);
        int deleted = repository.deleteByNationality(country);
        if (deleted == 0) {
          throw new NotFoundException("No people found with the specified nationality");
        }
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Provided nationality parameter is invalid");
      }

      return new Result.Success<>(null);
    } catch (Exception e) {
      return new Result.Error<>(e);
    }
  }
}
