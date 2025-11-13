package ru.ifmo.soa.peopleservice.resources;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import ru.ifmo.soa.peopleservice.dto.PeopleResponseDto;
import ru.ifmo.soa.peopleservice.dto.CallbackError;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SearchCallbackResource {

  @Inject
  private ObjectMapper objectMapper;

  public void sendResult(String taskId, String callbackUrl, PeopleResponseDto data, CallbackError error) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      Map<String, Object> payload = new HashMap<>();
      payload.put("correlationId", taskId);
      payload.put("timestamp", OffsetDateTime.now());
      if (error != null) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", error.code);
        errorDetails.put("message", error.message);
        payload.put("error", errorDetails);
      } else {
        payload.put("data", data);
      }
      HttpPost request = new HttpPost(callbackUrl);
      request.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));
      httpClient.execute(request);
    }
  }
}
