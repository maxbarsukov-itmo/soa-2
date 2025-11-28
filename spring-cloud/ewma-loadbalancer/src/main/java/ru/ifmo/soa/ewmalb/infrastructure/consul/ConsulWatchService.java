package ru.ifmo.soa.ewmalb.infrastructure.consul;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConsulWatchService {

  private static final Logger log = LoggerFactory.getLogger(ConsulWatchService.class);

  private final ConsulClient consulClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final EwmaLoadBalancer loadBalancer;
  private final String serviceName;
  private final int blockSeconds;

  private final AtomicLong lastIndex = new AtomicLong(1);
  private final Set<String> knownIds = new HashSet<>();

  private final ExecutorService executor;

  public ConsulWatchService(ConsulClient consulClient,
                            EwmaLoadBalancer loadBalancer,
                            @Value("${consul.watch.block-seconds}") int blockSeconds,
                            @Value("${service.backend.name}") String serviceName) {
    this.consulClient = consulClient;
    this.loadBalancer = loadBalancer;

    this.blockSeconds = blockSeconds;
    if (blockSeconds < 1 || blockSeconds > 600) {
      throw new IllegalArgumentException("consul.watch.block-seconds must be between 1 and 600 seconds");
    }

    this.serviceName = serviceName;
    if (serviceName == null || serviceName.trim().isEmpty()) {
      throw new IllegalArgumentException("service.backend.name must be set");
    }

    this.executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "consul-watch-" + serviceName + "-loop");
      t.setDaemon(true);
      return t;
    });
  }

  @PostConstruct
  public void startWatchLoop() {
    executor.submit(this::watchLoop);
  }

  private void watchLoop() {
    long retryDelayMs = 2000;
    final long maxRetryDelayMs = 30_000;

    while (!Thread.currentThread().isInterrupted()) {
      try {
        ConsulResponse resp = consulClient.blockingGet("/v1/health/service/" + serviceName, lastIndex.get(), blockSeconds);
        JsonNode response = objectMapper.readTree(resp.body());
        if (response.isEmpty()) {
          log.warn("Received empty service list from Consul");
          continue;
        }

        Set<String> currentIds = new HashSet<>();
        for (JsonNode node : response) {
          JsonNode checks = node.get("Checks");
          if (checks != null && !checks.isEmpty()) {
            JsonNode firstCheck = checks.get(0);
            if (!"passing".equals(firstCheck.path("Status").asText())) {
              continue;
            }
          } else {
            continue;
          }

          JsonNode svc = node.get("Service");
          if (svc == null) continue;

          String id = svc.path("ID").asText();
          String address = svc.path("Address").asText();
          int port = svc.path("Port").asInt();

          currentIds.add(id);
          if (!knownIds.contains(id)) {
            loadBalancer.registerInstance(id, address, port);
          }
        }

        for (String id : knownIds) {
          if (!currentIds.contains(id)) {
            loadBalancer.unregisterInstance(id);
          }
        }

        knownIds.clear();
        knownIds.addAll(currentIds);
        lastIndex.set(resp.consulIndex());
        loadBalancer.logCurrentState();

        retryDelayMs = 2000;
      } catch (IOException e) {
        log.warn("Consul watch I/O error (retrying in {} ms): {}", retryDelayMs, e.getMessage());
        knownIds.clear();
        loadBalancer.logCurrentState();
        handleRetryDelay(retryDelayMs);
        retryDelayMs = Math.min(retryDelayMs * 2, maxRetryDelayMs);
      } catch (Exception e) {
        log.error("Unexpected error in Consul watch (retrying in {} ms)", retryDelayMs, e);
        handleRetryDelay(retryDelayMs);
        retryDelayMs = Math.min(retryDelayMs * 2, maxRetryDelayMs);
      }
    }
    log.info("Consul watch loop stopped");
  }

  private void handleRetryDelay(long delayMs) {
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @PreDestroy
  public void stopWatchLoop() {
    executor.shutdownNow();
  }
}
