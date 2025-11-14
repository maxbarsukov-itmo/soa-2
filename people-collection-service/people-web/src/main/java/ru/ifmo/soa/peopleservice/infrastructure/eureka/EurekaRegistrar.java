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
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@Startup
public class EurekaRegistrar {

  private static final Logger LOG = Logger.getLogger(EurekaRegistrar.class.getName());

  private static final String APP_NAME = "PEOPLE-SERVICE";
  private static final String INSTANCE_ID = getInstanceId();
  private static final String HOST_NAME = getHostName();
  private static final int HEARTBEAT_INTERVAL = 30;

  // TODO FIXME http -> https
  private static final String eurekaBaseUrl = System.getenv().getOrDefault("EUREKA_URL", "http://localhost:8761/eureka/v2");
  private static final String serviceHost = "localhost";
  private static final int servicePort = 8080;
  private static final boolean securePort = false;
  private static final int securePortNumber = 51313;

  @Inject
  private ObjectMapper objectMapper;

  private CloseableHttpClient httpClient;
  private ScheduledExecutorService scheduler;

  // TODO FIXME http -> https
  @PostConstruct
  public void register() {
    httpClient = HttpClients.createDefault();
    String url = eurekaBaseUrl + "/apps/" + APP_NAME;

    Instance instance = new Instance();
    instance.instanceId = INSTANCE_ID;
    instance.hostName = HOST_NAME;
    instance.app = APP_NAME;
    instance.ipAddr = serviceHost;
    instance.vipAddress = "people-service";
    instance.secureVipAddress = "people-service";
    instance.status = "UP";
    instance.port = new Port();
    instance.port.value = servicePort;
    instance.port.enabled = "true";
    instance.securePort = new Port();
    instance.securePort.value = securePortNumber;
    instance.securePort.enabled = String.valueOf(securePort);
    instance.homePageUrl = "http://" + serviceHost + ":" + servicePort + "/";
    instance.statusPageUrl = "http://" + serviceHost + ":" + servicePort + "/info";
    instance.healthCheckUrl = "http://" + serviceHost + ":" + servicePort + "/health";
    instance.dataCenterInfo = new DataCenterInfo();
    instance.dataCenterInfo.name = "MyOwn";
    instance.leaseInfo = new LeaseInfo();

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
          LOG.info("Successfully registered in Eureka (v2)");
          startHeartbeat();
        } else {
          LOG.severe("Failed to register in Eureka: HTTP " + status);
          LOG.severe("Failed to register in Eureka: Response " + EntityUtils.toString(response.getEntity()));
        }
        return null;
      });
    } catch (IOException e) {
      LOG.severe("Exception during Eureka registration: " + e.getMessage());
    }
  }

  private void startHeartbeat() {
    scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
  }

  private void sendHeartbeat() {
    String url = eurekaBaseUrl + "/apps/" + APP_NAME + "/" + INSTANCE_ID;
    try {
      var request = ClassicRequestBuilder.put(url).build();
      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status != 200) {
          LOG.warning("Heartbeat failed: HTTP " + status);
        } else {
          LOG.info("Heartbeat: HTTP " + status);
        }
        return null;
      });
    } catch (IOException e) {
      LOG.warning("Heartbeat exception: " + e.getMessage());
    }
  }

  @PreDestroy
  public void deregister() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
    try {
      String url = eurekaBaseUrl + "/apps/" + APP_NAME + "/" + INSTANCE_ID;
      var request = ClassicRequestBuilder.delete(url).build();
      httpClient.execute(request, response -> {
        int status = response.getCode();
        if (status == 200) {
          LOG.info("Successfully deregistered from Eureka");
        } else {
          LOG.warning("Deregistration failed: HTTP " + status);
        }
        return null;
      });
      httpClient.close();
    } catch (IOException e) {
      LOG.warning("Deregistration exception: " + e.getMessage());
    }
  }

  private static String getInstanceId() {
    return getHostName() + ":" + APP_NAME + ":" + servicePort;
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "people-collection-service";
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
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class DataCenterInfo {
    @JsonProperty("@class")
    public String clazz = "com.netflix.appinfo.MyDataCenterInfo";
    public String name;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  static class LeaseInfo {
    public int renewalIntervalInSecs = HEARTBEAT_INTERVAL;
    public int durationInSecs = 3 * HEARTBEAT_INTERVAL;
  }
}
