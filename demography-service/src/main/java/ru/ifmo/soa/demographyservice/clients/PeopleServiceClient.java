package ru.ifmo.soa.demographyservice.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import ru.ifmo.soa.demographyservice.models.FilterCriteria;
import ru.ifmo.soa.demographyservice.models.FilterRule;
import ru.ifmo.soa.demographyservice.models.PeopleResponse;

import java.io.IOException;
import java.util.Collections;

public class PeopleServiceClient {

  // FIXME
  private static final String PEOPLE_GET_URL = "https://localhost:51313/api/v1/people?page=0&pageSize=1";
  private static final String PEOPLE_SEARCH_URL = "https://localhost:51313/api/v1/people/search?page=0&pageSize=1";

  private static final ObjectMapper mapper = new ObjectMapper();

  public static long getCountByHairColor(String hairColor) throws IOException {
    return getCountByField("hairColor", hairColor);
  }

  public static long getCountByEyeColor(String eyeColor) throws IOException {
    return getCountByField("eyeColor", eyeColor);
  }

  public static long getTotalCount() throws IOException {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(PEOPLE_GET_URL);
      var response = client.execute(request);
      if (response.getCode() != 200) {
        throw new IOException("People service (GET /people) returned: " + response.getCode());
      }
      PeopleResponse resp = mapper.readValue(response.getEntity().getContent(), PeopleResponse.class);
      return resp.getTotalCount() != null ? resp.getTotalCount() : 0L;
    }
  }

  private static long getCountByField(String field, String value) throws IOException {
    FilterRule rule = new FilterRule();
    rule.setField(field);
    rule.setOperator("eq");
    rule.setValue(value);

    FilterCriteria criteria = new FilterCriteria();
    criteria.setFilters(Collections.singletonList(rule));

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpPost request = new HttpPost(PEOPLE_SEARCH_URL);
      request.setHeader("Content-Type", "application/json");

      String json = mapper.writeValueAsString(criteria);
      request.setEntity(EntityBuilder.create()
        .setText(json)
        .setContentType(ContentType.APPLICATION_JSON)
        .build());

      var response = client.execute(request);
      if (response.getCode() != 200) {
        throw new IOException("People service (POST /search) returned: " + response.getCode());
      }

      PeopleResponse resp = mapper.readValue(response.getEntity().getContent(), PeopleResponse.class);
      return resp.getTotalCount() != null ? resp.getTotalCount() : 0L;
    }
  }
}
