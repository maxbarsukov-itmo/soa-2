package ru.ifmo.soa.demographyservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import ru.ifmo.soa.demographyservice.client.model.PeopleSearchRequest;
import ru.ifmo.soa.demographyservice.config.qualifier.BaseUrl;
import ru.ifmo.soa.demographyservice.dto.PeopleResponseDto;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class PeopleApiClient implements PeopleClient {

  private static final String GET_PEOPLE = "/people?page=0&pageSize=1";
  private static final String SEARCH_PEOPLE = "/people/search?page=0&pageSize=1";

  private final CloseableHttpClient httpClient;
  private final ObjectMapper mapper;
  private final String baseUrl;
  private final HttpClientResponseHandler<PeopleResponseDto> responseHandler;

  @Inject
  public PeopleApiClient(
    @BaseUrl String baseUrl,
    CloseableHttpClient httpClient,
    ObjectMapper mapper
  ) {
    this.baseUrl = baseUrl;
    this.httpClient = httpClient;
    this.mapper = mapper;
    this.responseHandler = createResponseHandler();
  }

  private HttpClientResponseHandler<PeopleResponseDto> createResponseHandler() {
    return response -> {
      if (response.getCode() != HttpStatus.SC_OK) {
        throw new IOException("Request to People API failed: HTTP " + response.getCode());
      }
      return mapper.readValue(response.getEntity().getContent(), PeopleResponseDto.class);
    };
  }

  public long getTotalCount() throws IOException {
    HttpGet request = new HttpGet(baseUrl + GET_PEOPLE);
    PeopleResponseDto dto = httpClient.execute(request, responseHandler);
    return dto.totalCount() != null ? dto.totalCount() : 0L;
  }

  public long getCountByField(String field, String value) throws IOException {
    var request = new PeopleSearchRequest(
      List.of(new PeopleSearchRequest.FilterRule(field, "eq", value))
    );

    String json = mapper.writeValueAsString(request);

    HttpPost post = new HttpPost(baseUrl + SEARCH_PEOPLE);
    post.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
    post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

    PeopleResponseDto dto = httpClient.execute(post, responseHandler);
    return dto.totalCount() != null ? dto.totalCount() : 0L;
  }
}
