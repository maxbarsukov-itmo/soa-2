package ru.ifmo.soa.demographyservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import ru.ifmo.soa.demographyservice.config.qualifier.*;

@ApplicationScoped
public class ApplicationConfig {

  private final String peopleServiceBaseUrl = System.getenv("PEOPLE_SERVICE_URL");
  private final int httpClientConnectTimeoutSec = getIntEnv("HTTP_CLIENT_CONNECT_TIMEOUT", 5);
  private final int httpClientResponseTimeoutSec = getIntEnv("HTTP_CLIENT_RESPONSE_TIMEOUT", 10);
  private final int httpClientMaxTotal = getIntEnv("HTTP_CLIENT_MAX_TOTAL", 10);
  private final int httpClientMaxPerRoute = getIntEnv("HTTP_CLIENT_MAX_PER_ROUTE", 5);

  private static int getIntEnv(String name, int defaultValue) {
    String val = System.getenv(name);
    if (val != null) {
      try {
        return Integer.parseInt(val);
      } catch (NumberFormatException ignored) {}
    }
    return defaultValue;
  }

  @Produces
  public ObjectMapper produceObjectMapper() {
    return new ObjectMapper();
  }

  @Produces @BaseUrl
  public String producePeopleServiceBaseUrl() {
    return peopleServiceBaseUrl != null ? peopleServiceBaseUrl : "https://localhost:51313/api/v1";
  }

  @Produces @ConnectTimeoutSec
  public Integer produceHttpClientConnectTimeoutSec() {
    return httpClientConnectTimeoutSec;
  }

  @Produces @ResponseTimeoutSec
  public Integer produceHttpClientResponseTimeoutSec() {
    return httpClientResponseTimeoutSec;
  }

  @Produces @HttpClientMaxTotal
  public Integer produceHttpClientMaxTotal() {
    return httpClientMaxTotal;
  }

  @Produces @HttpClientMaxPerRoute
  public Integer produceHttpClientMaxPerRoute() {
    return httpClientMaxPerRoute;
  }
}
