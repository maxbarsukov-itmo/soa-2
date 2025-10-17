package ru.ifmo.soa.peopleservice.resources;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import ru.ifmo.soa.peopleservice.exceptions.CallbackError;
import ru.ifmo.soa.peopleservice.models.PeopleResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SearchCallbackResource {

  public static void sendResult(String taskId, String callbackUrl, PeopleResponse data, CallbackError error) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> payload = new HashMap<>();
      payload.put("correlationId", "req-" + System.currentTimeMillis());
      payload.put("timestamp", LocalDateTime.now().toString());

      if (error != null) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", error.code);
        errorDetails.put("message", error.message);
        payload.put("error", errorDetails);
      } else {
        payload.put("data", data);
      }

      HttpPost request = new HttpPost(callbackUrl);
      request.setEntity(new StringEntity(mapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));

      httpClient.execute(request);
    }
  }
}
