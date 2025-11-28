package ru.ifmo.soa.demographyservice.infrastructure.consul;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class ConsulRegistrar {

  private static final Logger log = LoggerFactory.getLogger(ConsulRegistrar.class);

  private final Environment env;

  private final String serviceName = "demography-service";
  private final String consulHost;
  private final int consulPort;

  private String serviceHost;
  private int servicePort;
  private String serviceId;
  private String healthCheckUrl;

  private CloseableHttpClient httpClient;

  public ConsulRegistrar(Environment env) {
    this.env = env;
    this.consulHost = env.getProperty("consul.host");
    this.consulPort = env.getProperty("consul.port", Integer.class);
  }

  @PostConstruct
  public void register() {
    this.servicePort = env.getProperty("server.port", Integer.class);
    this.serviceHost = env.getProperty("service.host");
    this.serviceId = serviceName + "-" + UUID.randomUUID().toString().substring(0, 8);
    this.healthCheckUrl = "http://" + serviceHost + ":" + servicePort + "/api/v1/health";

    httpClient = HttpClients.createDefault();
    String registrationUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/register";

    String payload = """
      {
        "Name": "%s",
        "ID": "%s",
        "Address": "%s",
        "Port": %d,
        "Check": {
          "HTTP": "%s",
          "Interval": "10s",
          "Timeout": "3s"
        }
      }
      """.formatted(serviceName, serviceId, serviceHost, servicePort, healthCheckUrl);

    try {
      var request = ClassicRequestBuilder.put(registrationUrl)
        .setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON))
        .build();

      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status >= 200 && status < 300) {
          log.info("Successfully registered in Consul");
        } else {
          log.error("Failed to register in Consul: HTTP {}", status);
        }
        return null;
      });
    } catch (IOException e) {
      log.error("Exception during Consul registration: {}", e.getMessage());
    }
  }

  @PreDestroy
  public void close() {
    if (serviceId == null) return;

    String deregisterUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/deregister/" + serviceId;

    try {
      var request = ClassicRequestBuilder.put(deregisterUrl).build();
      httpClient.execute(request, response -> {
        log.info("Deregistered from Consul");
        return null;
      });
    } catch (IOException e) {
      log.error("Failed to deregister from Consul: {}", e.getMessage());
    } finally {
      try {
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (IOException e) {
        log.error("Failed to close HTTP client: {}", e.getMessage());
      }
    }
  }
}
