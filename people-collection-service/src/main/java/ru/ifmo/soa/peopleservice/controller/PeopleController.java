package ru.ifmo.soa.peopleservice.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import ru.ifmo.soa.peopleservice.dto.FilterCriteriaDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.dto.PersonInputDto;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.interactor.*;

import java.util.Map;

@Path("/people")
@Produces(MediaType.APPLICATION_JSON)
public class PeopleController {

  @Inject
  private GetAllPeopleInteractor getAllPeopleInteractor;

  @Inject
  private AddPersonInteractor addPersonInteractor;

  @Inject
  private GetPersonByIdInteractor getPersonByIdInteractor;

  @Inject
  private UpdatePersonInteractor updatePersonInteractor;

  @Inject
  private DeletePersonInteractor deletePersonInteractor;

  @Inject
  private DeletePeopleByNationalityInteractor deletePeopleByNationalityInteractor;

  @Inject
  private DeletePersonByLocationInteractor deletePersonByLocationInteractor;

  @Inject
  private GetPeopleWithLocationGreaterThanInteractor getPeopleWithLocationGreaterThanInteractor;

  @Inject
  private SearchPeopleInteractor searchPeopleInteractor;

  @GET
  public Response getPeople(
    @QueryParam("sortBy") String sortBy,
    @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
    @QueryParam("page") @DefaultValue("0") Integer page,
    @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
    return getAllPeopleInteractor.execute(sortBy, sortOrder, page, pageSize);
  }

  @POST
  public Response addPerson(@Valid PersonInputDto personInput) {
    return addPersonInteractor.execute(personInput);
  }

  @GET
  @Path("/{id}")
  public Response getPerson(@PathParam("id") Long id) {
    return getPersonByIdInteractor.execute(id);
  }

  @PATCH
  @Path("/{id}")
  public Response updatePerson(@PathParam("id") Long id, Map<String, Object> updates) {
    return updatePersonInteractor.execute(id, updates);
  }

  @DELETE
  @Path("/{id}")
  public Response deletePerson(@PathParam("id") Long id) {
    return deletePersonInteractor.execute(id);
  }

  @DELETE
  @Path("/nationality/{nationality}")
  public Response deletePeopleByNationality(@PathParam("nationality") String nationality) {
    return deletePeopleByNationalityInteractor.execute(nationality);
  }

  @DELETE
  @Path("/location")
  public Response deleteOnePersonByLocation(@Valid Location location) {
    return deletePersonByLocationInteractor.execute(location);
  }

  @GET
  @Path("/location/greater")
  public Response getPeopleWithLocationGreaterThan(
    @QueryParam("x") @NotNull Integer x,
    @QueryParam("y") @NotNull Long y,
    @QueryParam("z") @NotNull Integer z) {
    return getPeopleWithLocationGreaterThanInteractor.execute(x, y, z);
  }

  @POST
  @Path("/search")
  public Response searchPeople(
    @Valid FilterCriteriaDto filterCriteria,
    @QueryParam("sortBy") String sortBy,
    @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
    @QueryParam("page") @DefaultValue("0") Integer page,
    @QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
    @HeaderParam("X-Callback-URL") String callbackUrl) {
    return searchPeopleInteractor.execute(filterCriteria, sortBy, sortOrder, page, pageSize, callbackUrl);
  }
}
