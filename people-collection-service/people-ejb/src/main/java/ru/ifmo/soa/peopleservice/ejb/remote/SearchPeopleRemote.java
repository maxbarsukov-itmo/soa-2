package ru.ifmo.soa.peopleservice.ejb.remote;

import jakarta.ejb.Remote;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.FilterCriteriaDto;
import ru.ifmo.soa.peopleservice.util.Result;

@Remote
public interface SearchPeopleRemote {
  Result<PeopleResponseDto> searchPeople(
    FilterCriteriaDto filterCriteria,
    String sortBy,
    String sortOrder,
    Integer page,
    Integer pageSize
  );
}
