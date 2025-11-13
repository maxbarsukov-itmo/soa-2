package ru.ifmo.soa.peopleservice.adapters;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import ru.ifmo.soa.peopleservice.dto.*;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.ejb.remote.*;
import ru.ifmo.soa.peopleservice.util.Result;

import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
public class PersonServiceAdapter {

  @EJB(lookup = "java:global/people-ejb/AddPersonBean!ru.ifmo.soa.peopleservice.ejb.remote.AddPersonRemote")
  private AddPersonRemote addPersonService;

  @EJB(lookup = "java:global/people-ejb/GetAllPeopleBean!ru.ifmo.soa.peopleservice.ejb.remote.GetAllPeopleRemote")
  private GetAllPeopleRemote getAllPeopleService;

  @EJB(lookup = "java:global/people-ejb/GetPersonByIdBean!ru.ifmo.soa.peopleservice.ejb.remote.GetPersonByIdRemote")
  private GetPersonByIdRemote getPersonByIdService;

  @EJB(lookup = "java:global/people-ejb/UpdatePersonBean!ru.ifmo.soa.peopleservice.ejb.remote.UpdatePersonRemote")
  private UpdatePersonRemote updatePersonService;

  @EJB(lookup = "java:global/people-ejb/DeletePersonBean!ru.ifmo.soa.peopleservice.ejb.remote.DeletePersonRemote")
  private DeletePersonRemote deletePersonService;

  @EJB(lookup = "java:global/people-ejb/DeletePeopleByNationalityBean!ru.ifmo.soa.peopleservice.ejb.remote.DeletePeopleByNationalityRemote")
  private DeletePeopleByNationalityRemote deletePeopleByNationalityService;

  @EJB(lookup = "java:global/people-ejb/DeletePersonByLocationBean!ru.ifmo.soa.peopleservice.ejb.remote.DeletePersonByLocationRemote")
  private DeletePersonByLocationRemote deletePersonByLocationService;

  @EJB(lookup = "java:global/people-ejb/GetPeopleWithLocationGreaterThanBean!ru.ifmo.soa.peopleservice.ejb.remote.GetPeopleWithLocationGreaterThanRemote")
  private GetPeopleWithLocationGreaterThanRemote getPeopleWithLocationGreaterThanService;

  @EJB(lookup = "java:global/people-ejb/SearchPeopleBean!ru.ifmo.soa.peopleservice.ejb.remote.SearchPeopleRemote")
  private SearchPeopleRemote searchPeopleService;

  private <T> T unwrap(Supplier<Result<T>> supplier) {
    Result<T> result = supplier.get();
    if (result instanceof Result.Success<T> success) {
      return success.value();
    } else if (result instanceof Result.Error<T> error) {
      throw unwrapException(error.exception());
    }
    throw new IllegalStateException("Unexpected Result subtype: " + result.getClass());
  }

  private RuntimeException unwrapException(Exception ex) {
    if (ex instanceof RuntimeException re) {
      return re;
    }
    return new RuntimeException(ex);
  }

  public PersonDto addPerson(PersonInputDto dto) {
    return unwrap(() -> addPersonService.addPerson(dto));
  }

  public PeopleResponseDto getAllPeople(String sortBy, String sortOrder, Integer page, Integer pageSize) {
    return unwrap(() -> getAllPeopleService.getAllPeople(sortBy, sortOrder, page, pageSize));
  }

  public PersonDto getPersonById(Long id) {
    return unwrap(() -> getPersonByIdService.getPersonById(id));
  }

  public PersonDto updatePerson(Long id, Map<String, Object> updates) {
    return unwrap(() -> updatePersonService.updatePerson(id, updates));
  }

  public void deletePerson(Long id) {
    unwrap(() -> deletePersonService.deletePerson(id));
  }

  public void deletePeopleByNationality(String nationality) {
    unwrap(() -> deletePeopleByNationalityService.deletePeopleByNationality(nationality));
  }

  public void deletePersonByLocation(Location location) {
    unwrap(() -> deletePersonByLocationService.deletePersonByLocation(location));
  }

  public PeopleResponseDto getPeopleWithLocationGreaterThan(Integer x, Long y, Integer z) {
    return unwrap(() -> getPeopleWithLocationGreaterThanService.getPeopleWithLocationGreaterThan(x, y, z));
  }

  public PeopleResponseDto searchPeople(
    FilterCriteriaDto filterCriteria,
    String sortBy,
    String sortOrder,
    Integer page,
    Integer pageSize
  ) {
    return unwrap(() -> searchPeopleService.searchPeople(filterCriteria, sortBy, sortOrder, page, pageSize));
  }
}
