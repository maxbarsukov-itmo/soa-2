package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.util.Result;

import java.util.Map;

@Remote
public interface UpdatePersonRemote {
  Result<PersonDto> updatePerson(Long id, Map<String, Object> updates);
}
