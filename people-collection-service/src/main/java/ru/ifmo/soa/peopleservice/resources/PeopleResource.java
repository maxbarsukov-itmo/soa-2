package ru.ifmo.soa.peopleservice.resources;

import jakarta.validation.constraints.Min;
import ru.ifmo.soa.peopleservice.entities.Location;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.exceptions.BadRequestException;
import ru.ifmo.soa.peopleservice.exceptions.NotFoundException;
import ru.ifmo.soa.peopleservice.models.*;
import ru.ifmo.soa.peopleservice.repositories.PersonRepository;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/people")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PeopleResource {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "id", "name", "creationDate", "coordinates.x", "coordinates.y",
    "height", "eyeColor", "hairColor", "nationality",
    "location.x", "location.y", "location.z", "location.name"
  );

  private final PersonRepository repository = new PersonRepository();
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  @GET
  public Response getPeople(
    @QueryParam("sortBy") String sortBy,
    @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
    @QueryParam("page") @DefaultValue("0") @Min(0) Integer page,
    @QueryParam("pageSize") @DefaultValue("10") @Min(1) Integer pageSize,
    @Context UriInfo uriInfo) {

    if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
      throw new BadRequestException("Invalid sortBy field: " + sortBy);
    }

    try {
      List<Person> people = repository.findAll(page, pageSize, sortBy, sortOrder);
      long totalCount = repository.countAll();
      int totalPages = (int) Math.ceil((double) totalCount / pageSize);

      PeopleResponse response = new PeopleResponse(people, page, pageSize, totalPages, totalCount);
      return Response.ok(response).build();
    } catch (NumberFormatException e) {
      throw new BadRequestException("One or more query parameters contain invalid values");
    } catch (Exception e) {
      if (e instanceof SemanticException) {
        throw e;
      }
      throw new InternalServerErrorException("An unexpected error occurred while fetching people");
    }
  }

  @POST
  public Response addPerson(@Valid PersonInput personInput) {
    try {
      if (personInput.getHeight() != null && personInput.getHeight() <= 0) {
        throw new SemanticException("Request contains semantic errors (e.g., invalid data for field)");
      }
      if (repository.isStorageFull()) {
        throw new InsufficientStorageException("Server storage capacity exceeded");
      }
      Person person = new Person(
        personInput.getName(),
        personInput.getCoordinates(),
        personInput.getHeight(),
        personInput.getEyeColor(),
        personInput.getHairColor(),
        personInput.getNationality(),
        personInput.getLocation()
      );
      repository.save(person);
      return Response.status(Response.Status.CREATED).entity(person).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Request body contains malformed JSON or invalid data format");
    } catch (ConflictException e) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(409, e.getMessage())).build();
    } catch (ContentTooLargeException e) {
      return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).entity(new ErrorResponse(413, e.getMessage())).build();
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponse(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponse(422, e.getMessage())).build();
    } catch (InsufficientStorageException e) {
      return Response.status(507, "Insufficient Storage").entity(new ErrorResponse(507, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred while creating the person")).build();
    }
  }

  @GET
  @Path("/{id}")
  public Response getPerson(@PathParam("id") Long id) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Provided ID parameter is invalid");
      }
      Person person = repository.findById(id);
      return Response.ok(person).build();
    } catch (BadRequestException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(400, e.getMessage())).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred while retrieving the person")).build();
    }
  }

  @PATCH
  @Path("/{id}")
  public Response updatePerson(@PathParam("id") Long id, @Valid PersonInput personInput) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Invalid ID parameter or request body");
      }
      Person existingPerson = repository.findById(id);
      existingPerson.setName(personInput.getName());
      existingPerson.setCoordinates(personInput.getCoordinates());
      existingPerson.setHeight(personInput.getHeight());
      existingPerson.setEyeColor(personInput.getEyeColor());
      existingPerson.setHairColor(personInput.getHairColor());
      existingPerson.setNationality(personInput.getNationality());
      existingPerson.setLocation(personInput.getLocation());
      repository.update(existingPerson);
      return Response.ok(existingPerson).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid ID parameter or request body");
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(404, e.getMessage())).build();
    } catch (MethodNotAllowedException e) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(new ErrorResponse(405, e.getMessage())).build();
    } catch (ConflictException e) {
      return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(409, e.getMessage())).build();
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponse(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponse(422, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred while updating the person")).build();
    }
  }

  @DELETE
  @Path("/{id}")
  public Response deletePerson(@PathParam("id") Long id) {
    try {
      if (id == null || id <= 0) {
        throw new BadRequestException("Provided ID parameter is invalid");
      }
      repository.deleteById(id);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (BadRequestException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(400, e.getMessage())).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred while deleting the person")).build();
    }
  }

  @DELETE
  @Path("/nationality/{nationality}")
  public Response deletePeopleByNationality(@PathParam("nationality") String nationality) {
    try {
      repository.deleteByNationality(nationality);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Provided nationality parameter is invalid");
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred during bulk deletion")).build();
    }
  }

  @DELETE
  @Path("/location")
  public Response deleteOnePersonByLocation(@Valid Location location) {
    try {
      repository.deleteByLocation(location);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Location data in request body is invalid or malformed");
    } catch (UnsupportedMediaTypeException e) {
      return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity(new ErrorResponse(415, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponse(422, e.getMessage())).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(404, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred during location-based deletion")).build();
    }
  }

  @GET
  @Path("/location/greater")
  public Response getPeopleWithLocationGreaterThan(
    @QueryParam("x") Integer x,
    @QueryParam("y") Long y,
    @QueryParam("z") Integer z) {
    try {
      if (x == null || y == null || z == null) {
        throw new BadRequestException("One or more location parameters are invalid");
      }
      List<Person> people = repository.findWithLocationGreaterThan(x, y, z);
      long totalCount = people.size();
      int totalPages = (int) Math.ceil((double) totalCount / 10.0);
      PeopleResponse response = new PeopleResponse(people, 0, 10, totalPages, totalCount);
      return Response.ok(response).build();
    } catch (BadRequestException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(400, e.getMessage())).build();
    } catch (SemanticException e) {
      return Response.status(422, "Unprocessable Entity").entity(new ErrorResponse(422, e.getMessage())).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred during location comparison")).build();
    }
  }

  @POST
  @Path("/search")
  public Response searchPeople(
    @Valid FilterCriteria filterCriteria,
    @QueryParam("sortBy") String sortBy,
    @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
    @QueryParam("page") @DefaultValue("0") @Min(0) Integer page,
    @QueryParam("pageSize") @DefaultValue("10") @Min(1) Integer pageSize,
    @HeaderParam("X-Callback-URL") String callbackUrl) {

    if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
      throw new BadRequestException("Invalid sortBy field: " + sortBy);
    }

    if (callbackUrl != null) {
      CompletableFuture.runAsync(() -> {
        try {
          List<Person> people = repository.findWithFilters(filterCriteria, page, pageSize, sortBy, sortOrder);
          long totalCount = repository.countWithFilters(filterCriteria);
          int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
          PeopleResponse response = new PeopleResponse(people, page, pageSize, totalPages, totalCount);

          SearchCallbackResource.sendResult(callbackUrl, response, null);
        } catch (Exception e) {
          try {
            SearchCallbackResource.sendResult(callbackUrl, null, new CallbackError(500, e.getMessage()));
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
      }, executorService);

      return Response.status(Response.Status.ACCEPTED)
        .entity(new ErrorResponse(202, "Search task accepted. Results will be sent to your callback URL."))
        .build();
    } else {
      try {
        List<Person> people = repository.findWithFilters(filterCriteria, page, pageSize, sortBy, sortOrder);
        long totalCount = repository.countWithFilters(filterCriteria);
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);

        PeopleResponse response = new PeopleResponse(people, page, pageSize, totalPages, totalCount);
        return Response.ok(response).build();
      } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid query parameters or malformed JSON in request body");
      } catch (Exception e) {
        if (e instanceof SemanticException) {
          return Response.status(422, "Unprocessable Entity").entity(new ErrorResponse(422, e.getMessage())).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(500, "An unexpected error occurred while processing the search")).build();
      }
    }
  }
}
