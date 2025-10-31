package ru.ifmo.soa.peopleservice.interactor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.peopleservice.dto.AsyncSearchResponseDto;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;
import ru.ifmo.soa.peopleservice.dto.FilterCriteriaDto;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.PersonDto;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.*;
import ru.ifmo.soa.peopleservice.mapper.PersonMapper;
import ru.ifmo.soa.peopleservice.repository.PersonRepository;
import ru.ifmo.soa.peopleservice.resources.SearchCallbackResource;
import ru.ifmo.soa.peopleservice.util.PathResolver;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class SearchPeopleInteractor {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
    "id", "name", "creationDate", "coordinates.x", "coordinates.y",
    "height", "eyeColor", "hairColor", "nationality",
    "location.x", "location.y", "location.z", "location.name"
  );
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  @Inject
  private PersonRepository repository;
  @Inject
  private PersonMapper mapper;
  @Inject
  private SearchCallbackResource callbackResource;

  @Transactional
  public Response execute(FilterCriteriaDto filterCriteria, String sortBy, String sortOrder, Integer page, Integer pageSize, String callbackUrl) {
    if (sortBy != null && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
      throw new BadRequestException("Invalid sortBy field: " + sortBy);
    }
    if (pageSize < 0) {
      throw new SemanticException("Page size cannot be negative");
    }
    if (page < 0) {
      throw new SemanticException("Page number cannot be negative");
    }
    if (callbackUrl != null) {
      try {
        new java.net.URL(callbackUrl).toURI();
      } catch (Exception e) {
        throw new BadRequestException("Invalid callback URL format");
      }
      String taskId = "task-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
      CompletableFuture.runAsync(() -> {
        try {
          PathResolver.SortInfo sortInfo = new PathResolver.SortInfo(sortBy, sortOrder);
          List<Person> people = repository.findWithFilters(filterCriteria, page, pageSize, sortInfo);
          long totalCount = repository.countWithFilters(filterCriteria);
          int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
          List<PersonDto> personDtos = mapper.toDtoList(people);
          PeopleResponseDto response = new PeopleResponseDto(personDtos, page, pageSize, totalPages, totalCount);
          callbackResource.sendResult(taskId, callbackUrl, response, null);
        } catch (Exception e) {
          try {
            callbackResource.sendResult(taskId, callbackUrl, null, new CallbackError(500, e.getMessage()));
          } catch (java.io.IOException ex) {
            System.err.println("Failed to send callback error: " + ex.getMessage());
          }
        }
      }, executorService);
      AsyncSearchResponseDto response = new AsyncSearchResponseDto(
        taskId,
        "Search task accepted. Results will be sent to your callback URL.",
        java.time.OffsetDateTime.now().plusMinutes(5).toString()
      );
      return Response.status(Response.Status.ACCEPTED)
        .entity(response)
        .build();
    } else {
      try {
        PathResolver.SortInfo sortInfo = new PathResolver.SortInfo(sortBy, sortOrder);
        List<Person> people = repository.findWithFilters(filterCriteria, page, pageSize, sortInfo);
        long totalCount = repository.countWithFilters(filterCriteria);
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
        List<PersonDto> personDtos = mapper.toDtoList(people);
        PeopleResponseDto response = new PeopleResponseDto(personDtos, page, pageSize, totalPages, totalCount);
        return Response.ok(response).build();
      } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid query parameters or malformed JSON in request body");
      } catch (Exception e) {
        if (e instanceof SemanticException) {
          return Response.status(422, "Unprocessable Entity").entity(new ErrorResponseDto(422, e.getMessage())).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponseDto(500, "An unexpected error occurred while processing the search")).build();
      }
    }
  }
}
