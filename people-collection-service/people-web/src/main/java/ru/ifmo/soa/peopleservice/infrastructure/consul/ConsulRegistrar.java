package ru.ifmo.soa.peopleservice.infrastructure.consul;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.util.logging.Logger;

@Singleton
@Startup
public class ConsulRegistrar {

  private static final Logger LOG = Logger.getLogger(ConsulRegistrar.class.getName());

  private static final String SERVICE_NAME = "people-service";
  private static final String CONSUL_HOST = System.getenv().getOrDefault("CONSUL_HOST", "localhost");
  private static final int CONSUL_PORT = Integer.parseInt(System.getenv().getOrDefault("CONSUL_PORT", "8500"));

  // TODO FIXME https doesnt work
  private static final String HEALTH_CHECK_URL = "http://172.17.0.1:8080/api/v1/health";
  private static final String SERVICE_ADDRESS = "172.17.0.1";
  private static final int SERVICE_PORT = 8080;

  private CloseableHttpClient httpClient;

  @PostConstruct
  public void register() {
    httpClient = HttpClients.createDefault();
    String registrationUrl = "http://" + CONSUL_HOST + ":" + CONSUL_PORT + "/v1/agent/service/register";

    String payload = """
      {
        "Name": "%s",
        "ID": "%s-1",
        "Address": "%s",
        "Port": %d,
        "Check": {
          "HTTP": "%s",
          "Interval": "10s",
          "Timeout": "3s"
        }
      }
      """.formatted(SERVICE_NAME, SERVICE_NAME, SERVICE_ADDRESS, SERVICE_PORT, HEALTH_CHECK_URL);

    try {
      var request = ClassicRequestBuilder.put(registrationUrl)
        .setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON))
        .build();
      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status >= 200 && status < 300) {
          LOG.info("Successfully registered in Consul");
        } else {
          LOG.severe("Failed to register in Consul: HTTP " + status);
        }
        return null;
      });
    } catch (IOException e) {
      LOG.severe("Exception during Consul registration: " + e.getMessage());
    }
  }

  @PreDestroy
  public void close() {
    String deregisterUrl = "http://" + CONSUL_HOST + ":" + CONSUL_PORT + "/v1/agent/service/deregister/" + SERVICE_NAME + "-1";
    try {
      var request = ClassicRequestBuilder.put(deregisterUrl).build();
      httpClient.execute(request, response -> {
        LOG.info("Deregistered from Consul");
        return null;
      });
    } catch (IOException e) {
      LOG.warning("Failed to deregister from Consul: " + e.getMessage());
    } finally {
      try {
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (IOException e) {
        LOG.warning("Failed to close HTTP client: " + e.getMessage());
      }
    }
  }
}
