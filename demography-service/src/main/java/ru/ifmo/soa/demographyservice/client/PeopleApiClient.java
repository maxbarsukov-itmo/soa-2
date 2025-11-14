package ru.ifmo.soa.demographyservice.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.ifmo.soa.demographyservice.client.model.PeopleSearchRequest;
import ru.ifmo.soa.demographyservice.dto.PeopleResponseDto;

@Component
public class PeopleApiClient implements PeopleClient {

  private static final String BASE_URL = "http://people-service/api/v1";
  private static final String GET_PEOPLE = BASE_URL + "/people?page=0&pageSize=1";
  private static final String SEARCH_PEOPLE = BASE_URL + "/people/search?page=0&pageSize=1";

  private final RestTemplate restTemplate;

  public PeopleApiClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public long getCountByField(String field, String value) {
    var request = new PeopleSearchRequest(
      java.util.List.of(new PeopleSearchRequest.FilterRule(field, "eq", value))
    );

    var response = restTemplate.postForObject(SEARCH_PEOPLE, request, PeopleResponseDto.class);
    return response != null ? response.totalCount() : 0L;
  }

  public long getTotalCount() {
    var response = restTemplate.getForObject(GET_PEOPLE, PeopleResponseDto.class);
    return response != null ? response.totalCount() : 0L;
  }
}
