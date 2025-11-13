package ru.ifmo.soa.peopleservice.resources;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.*;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.adapters.PersonServiceAdapter;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/people")
@Produces(MediaType.APPLICATION_JSON)
public class PeopleResource {

  private final ExecutorService executorService = Executors.newCachedThreadPool();

  @Inject
  private PersonServiceAdapter personService;

  @Inject
  private SearchCallbackResource callbackResource;

  @GET
  public Response getPeople(
    @QueryParam("sortBy") String sortBy,
    @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
    @QueryParam("page") @DefaultValue("0") Integer page,
    @QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
    PeopleResponseDto response = personService.getAllPeople(sortBy, sortOrder, page, pageSize);
    return Response.ok(response).build();
  }

  @POST
  public Response addPerson(@Valid PersonInputDto personInput) {
    PersonDto dto = personService.addPerson(personInput);
    return Response.status(Response.Status.CREATED).entity(dto).build();
  }

  @GET
  @Path("/{id}")
  public Response getPerson(@PathParam("id") Long id) {
    PersonDto dto = personService.getPersonById(id);
    return Response.ok(dto).build();
  }

  @PATCH
  @Path("/{id}")
  public Response updatePerson(@PathParam("id") Long id, Map<String, Object> updates) {
    PersonDto dto = personService.updatePerson(id, updates);
    return Response.ok(dto).build();
  }

  @DELETE
  @Path("/{id}")
  public Response deletePerson(@PathParam("id") Long id) {
    personService.deletePerson(id);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @DELETE
  @Path("/nationality/{nationality}")
  public Response deletePeopleByNationality(@PathParam("nationality") String nationality) {
    personService.deletePeopleByNationality(nationality);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @DELETE
  @Path("/location")
  public Response deleteOnePersonByLocation(@Valid Location location) {
    personService.deletePersonByLocation(location);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  @GET
  @Path("/location/greater")
  public Response getPeopleWithLocationGreaterThan(
    @QueryParam("x") @NotNull Integer x,
    @QueryParam("y") @NotNull Long y,
    @QueryParam("z") @NotNull Integer z) {
    PeopleResponseDto response = personService.getPeopleWithLocationGreaterThan(x, y, z);
    return Response.ok(response).build();
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

    if (callbackUrl != null) {
      try {
        new URL(callbackUrl).toURI();
      } catch (Exception e) {
        throw new BadRequestException("Invalid callback URL format");
      }

      String taskId = "task-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
      CompletableFuture.runAsync(() -> {
        try {
          PeopleResponseDto result = personService.searchPeople(filterCriteria, sortBy, sortOrder, page, pageSize);
          callbackResource.sendResult(taskId, callbackUrl, result, null);
        } catch (Exception e) {
          try {
            callbackResource.sendResult(taskId, callbackUrl, null, new CallbackError(500, e.getMessage()));
          } catch (IOException ex) {
            System.err.println("Failed to send callback error: " + ex.getMessage());
          }
        }
      }, executorService);

      AsyncSearchResponseDto response = new AsyncSearchResponseDto(
        taskId,
        "Search task accepted. Results will be sent to your callback URL.",
        OffsetDateTime.now().plusMinutes(5).toString()
      );
      return Response.status(Response.Status.ACCEPTED).entity(response).build();
    } else {
      PeopleResponseDto response = personService.searchPeople(filterCriteria, sortBy, sortOrder, page, pageSize);
      return Response.ok(response).build();
    }
  }
}
