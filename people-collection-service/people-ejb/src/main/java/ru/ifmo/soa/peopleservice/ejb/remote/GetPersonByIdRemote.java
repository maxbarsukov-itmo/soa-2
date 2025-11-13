package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface GetPersonByIdRemote {
  Result<PersonDto> getPersonById(Long id);
}
