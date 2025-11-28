package ru.ifmo.soa.peopleservice.infrastructure.eureka;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

@Singleton
@Startup
public class EurekaRegistrar {

  private static final Logger log = Logger.getLogger(EurekaRegistrar.class.getName());

  private final String appName = "people-service";
  private final int heartbeatInterval = 30; // seconds

  private final String eurekaBaseUrl;
  private final String serviceHost;
  private final int servicePort;
  private final String instanceId;

  @Inject
  private ObjectMapper objectMapper;

  private CloseableHttpClient httpClient;
  private ScheduledExecutorService scheduler;

  public EurekaRegistrar() {
    this.eurekaBaseUrl = System.getenv().get("EUREKA_URL");
    this.serviceHost = System.getenv().get("SERVICE_HOST");
    this.servicePort = getHttpPort();
    this.instanceId = appName + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @PostConstruct
  public void register() {
    httpClient = HttpClients.createDefault();
    String url = eurekaBaseUrl + "/apps/" + appName;

    Instance instance = new Instance();
    instance.instanceId = instanceId;
    instance.hostName = serviceHost;
    instance.app = appName;
    instance.ipAddr = serviceHost;
    instance.vipAddress = "people-service";
    instance.secureVipAddress = "people-service";
    instance.status = "UP";
    instance.port = new Port(servicePort, true);
    instance.securePort = new Port(51313, false);

    instance.homePageUrl = "http://" + serviceHost + ":" + servicePort + "/";
    instance.statusPageUrl = "http://" + serviceHost + ":" + servicePort + "/api/v1/health";
    instance.healthCheckUrl = "http://" + serviceHost + ":" + servicePort + "/api/v1/health";
    instance.dataCenterInfo = new DataCenterInfo("MyOwn");
    instance.leaseInfo = new LeaseInfo(heartbeatInterval, 3 * heartbeatInterval);

    RegistrationRequest request = new RegistrationRequest(instance);

    try {
      String json = objectMapper.writeValueAsString(request);
      var httpRequest = ClassicRequestBuilder.post(url)
        .setHeader("Content-Type", "application/json")
        .setEntity(new StringEntity(json, ContentType.APPLICATION_JSON))
        .build();

      httpClient.execute(httpRequest, response -> {
        int status = response.getCode();
        if (status == 204) { // Eureka returns 204 No Content on success
          log.info(String.format("Registered in Eureka (v2): ID=%s, Host=%s, Port=%s", instanceId, serviceHost, servicePort));
          startHeartbeat();
        } else {
          log.severe("Failed to register in Eureka: HTTP " + status);
          log.severe("Failed to register in Eureka: Response " + EntityUtils.toString(response.getEntity()));
        }
        return null;
      });
    } catch (IOException e) {
      log.severe("Exception during Eureka registration: " + e.getMessage());
    }
  }

  private void startHeartbeat() {
    scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(this::sendHeartbeat, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
  }

  private void sendHeartbeat() {
    String url = eurekaBaseUrl + "/apps/" + appName + "/" + instanceId;
    try {
      var request = ClassicRequestBuilder.put(url).build();
      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status != 200) {
          log.warning("Heartbeat failed: HTTP " + status);
        } else {
          log.info("Heartbeat OK: HTTP " + status);
        }
        return null;
      });
    } catch (IOException e) {
      log.warning("Heartbeat exception: " + e.getMessage());
    }
  }

  @PreDestroy
  public void deregister() {
    if (scheduler != null) {
      scheduler.shutdown();
    }

    try {
      String url = eurekaBaseUrl + "/apps/" + appName + "/" + instanceId;
      var request = ClassicRequestBuilder.delete(url).build();

      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status == 200) {
          log.info(String.format("Deregistered from Eureka: ID=%s", instanceId));
        } else {
          log.warning("Deregistration failed: HTTP " + status);
        }
        return null;
      });
      httpClient.close();
    } catch (IOException e) {
      log.warning("Deregistration exception: " + e.getMessage());
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

  // --- DTOs ---
  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class RegistrationRequest {
    public Instance instance;
    public RegistrationRequest(Instance instance) { this.instance = instance; }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class Instance {
    public String instanceId;
    public String hostName;
    public String app;
    public String ipAddr;
    public String vipAddress;
    public String secureVipAddress;
    public String status;
    public Port port;
    public Port securePort;
    public String homePageUrl;
    public String statusPageUrl;
    public String healthCheckUrl;
    public DataCenterInfo dataCenterInfo;
    public LeaseInfo leaseInfo;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class Port {
    @JsonProperty("$")
    public int value;
    @JsonProperty("@enabled")
    public String enabled;

    public Port(int value, boolean enabled) {
      this.value = value;
      this.enabled = String.valueOf(enabled);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class DataCenterInfo {
    @JsonProperty("@class")
    public String clazz = "com.netflix.appinfo.MyDataCenterInfo";
    public String name;

    public DataCenterInfo(String name) {
      this.name = name;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class LeaseInfo {
    public int renewalIntervalInSecs;
    public int durationInSecs;

    public LeaseInfo(int renewalIntervalInSecs, int durationInSecs) {
      this.renewalIntervalInSecs = renewalIntervalInSecs;
      this.durationInSecs = durationInSecs;
    }
  }
}
