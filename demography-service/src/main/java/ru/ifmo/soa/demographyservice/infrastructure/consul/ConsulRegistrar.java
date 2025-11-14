package ru.ifmo.soa.demographyservice.infrastructure.consul;

import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Component
public class ConsulRegistrar {

  private static final Logger LOG = LoggerFactory.getLogger(ConsulRegistrar.class);

  private static final String SERVICE_NAME = "demography-service";

  @Value("${consul.host}")
  private String consulHost;

  @Value("${consul.port}")
  private int consulPort;

  // TODO FIXME https?
  private static final String HEALTH_CHECK_URL = "http://172.17.0.1:8081/api/v1/health";
  private static final String SERVICE_ADDRESS = "172.17.0.1";
  private static final int SERVICE_PORT = 8081;

  private CloseableHttpClient httpClient;

  @PostConstruct
  public void register() {
    httpClient = HttpClients.createDefault();
    String registrationUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/register";

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
          LOG.error("Failed to register in Consul: HTTP {}", status);
        }
        return null;
      });
    } catch (IOException e) {
      LOG.error("Exception during Consul registration: {}", e.getMessage());
    }
  }

  @PreDestroy
  public void close() {
    String deregisterUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/deregister/" + SERVICE_NAME + "-1";
    try {
      var request = ClassicRequestBuilder.put(deregisterUrl).build();
      httpClient.execute(request, response -> {
        LOG.info("Deregistered from Consul");
        return null;
      });
    } catch (IOException e) {
      LOG.error("Failed to deregister from Consul: {}", e.getMessage());
    } finally {
      try {
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (IOException e) {
        LOG.error("Failed to close HTTP client: {}", e.getMessage());
      }
    }
  }
}
