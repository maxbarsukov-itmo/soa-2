package ru.ifmo.soa.ewmalb.infrastructure.consul;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class ConsulRegistrationService {

  private static final Logger log = LoggerFactory.getLogger(ConsulRegistrationService.class);
  private final String serviceName = "ewma-loadbalancer";
  private final String serviceId;

  @Value("${consul.host}")
  private String consulHost;

  @Value("${consul.port}")
  private int consulPort;

  @Value("${server.port}")
  private int serverPort;

  @Value("${service.host}")
  private String serverHost;

  @Value("${service.meta.version}")
  private String serviceVersion;

  @Value("${service.meta.type}")
  private String serviceType;

  private CloseableHttpClient httpClient;

  public ConsulRegistrationService() {
    this.serviceId = serviceName + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @PostConstruct
  public void register() {
    httpClient = HttpClients.createDefault();

    try {
      String healthCheckUrl = "http://" + serverHost + ":" + serverPort + "/health";
      String registrationUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/register";

      String payload = String.format("""
        {
          "Name": "%s",
          "ID": "%s",
          "Address": "%s",
          "Port": %d,
          "Tags": ["loadbalancer", "ewma"],
          "Check": {
            "HTTP": "%s",
            "Interval": "10s",
            "Timeout": "3s",
            "DeregisterCriticalServiceAfter": "1m"
          },
          "Meta": {
            "version": "%s",
            "type": "%s"
          }
        }
        """, serviceName, serviceId, serverHost, serverPort, healthCheckUrl, serviceVersion, serviceType);

      var request = ClassicRequestBuilder.put(registrationUrl)
        .setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON))
        .build();

      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status >= 200 && status < 300) {
          log.info("EWMA Load Balancer registered in Consul: {}:{}", serverHost, serverPort);
        } else {
          log.error("Failed to register in Consul: HTTP {}", status);
        }
        return null;
      });

    } catch (Exception e) {
      log.error("Exception during Consul registration: {}", e.getMessage());
    }
  }

  @PreDestroy
  public void deregister() {
    if (serviceId == null) return;

    String deregisterUrl = "http://" + consulHost + ":" + consulPort +
      "/v1/agent/service/deregister/" + serviceId;

    try {
      var request = ClassicRequestBuilder.put(deregisterUrl).build();
      httpClient.execute(request, response -> {
        log.info("EWMA Load Balancer deregistered from Consul");
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
