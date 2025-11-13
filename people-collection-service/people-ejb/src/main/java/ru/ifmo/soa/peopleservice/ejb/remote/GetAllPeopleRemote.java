package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface GetAllPeopleRemote {
  Result<PeopleResponseDto> getAllPeople(String sortBy, String sortOrder, Integer page, Integer pageSize);
}
