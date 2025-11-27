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
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

@Singleton
@Startup
public class ConsulRegistrar {

  private static final Logger LOG = Logger.getLogger(ConsulRegistrar.class.getName());

  private final String serviceName = "people-service";
  private final String consulHost = System.getenv().get("CONSUL_HOST");
  private final int consulPort = Integer.parseInt(System.getenv().get("CONSUL_PORT"));

  private final String serviceHost = System.getenv().getOrDefault("SERVICE_HOST", "172.17.0.1");
  private final int servicePort = getHttpPort();
  private final String serviceId = serviceName + "-" + UUID.randomUUID().toString().substring(0, 8);
  private final String healthCheckUrl = "http://" + serviceHost + ":" + servicePort + "/api/v1/health";

  private CloseableHttpClient httpClient;

  @PostConstruct
  public void register() {
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
    String deregisterUrl = "http://" + consulHost + ":" + consulPort + "/v1/agent/service/deregister/" + serviceId;
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

  private int getHttpPort() {
    try {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName binding = new ObjectName("jboss.as:socket-binding-group=standard-sockets,socket-binding=http");
        return (Integer) server.getAttribute(binding, "boundPort");
    } catch (Exception e) {
        String portStr = System.getProperty("jboss.http.port");
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Failed to determine HTTP port", e);
        }
    }
  }
}
