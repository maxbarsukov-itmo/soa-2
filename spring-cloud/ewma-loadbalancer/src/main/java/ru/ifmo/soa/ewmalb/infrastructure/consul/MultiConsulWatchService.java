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
import ru.ifmo.soa.ewmalb.config.ServiceDiscoveryConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MultiConsulWatchService {

  private static final Logger log = LoggerFactory.getLogger(MultiConsulWatchService.class);

  private final ConsulClient consulClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final EwmaLoadBalancer loadBalancer;

  private final List<String> serviceNames;
  private final int blockSeconds;

  private final long initialRetryDelayMs;
  private final long maxRetryDelayMs;

  private final ConcurrentHashMap<String, WatchLoop> activeLoops = new ConcurrentHashMap<>();

  public MultiConsulWatchService(ConsulClient consulClient,
                                EwmaLoadBalancer loadBalancer,
                                @Value("${consul.watch.block-seconds}") int blockSeconds,
                                @Value("${consul.watch.initial-retry-delay-ms:2000}") long initialRetryDelayMs,
                                @Value("${consul.watch.max-retry-delay-ms:30000}") long maxRetryDelayMs,
                                 ServiceDiscoveryConfig serviceConfig) {
    this.consulClient = consulClient;
    this.loadBalancer = loadBalancer;
    this.initialRetryDelayMs = initialRetryDelayMs;
    this.maxRetryDelayMs = maxRetryDelayMs;

    this.blockSeconds = blockSeconds;
    if (blockSeconds < 1 || blockSeconds > 600) {
      throw new IllegalArgumentException("consul.watch.block-seconds must be between 1 and 600 seconds");
    }

    this.serviceNames = new ArrayList<>(serviceConfig.getBackends());
    if (serviceNames.isEmpty()) {
      throw new IllegalArgumentException("At least one service must be specified in service.backends");
    }
  }

  @PostConstruct
  public void startWatchLoops() {
    for (String serviceName : serviceNames) {
      WatchLoop loop = new WatchLoop(serviceName);
      activeLoops.put(serviceName, loop);
      loop.start();
    }
  }

  @PreDestroy
  public void stopWatchLoops() {
    activeLoops.values().forEach(WatchLoop::stop);
  }

  private class WatchLoop {

    private final String serviceName;
    private final ExecutorService executor;

    private volatile boolean running = true;
    private final AtomicLong lastIndex = new AtomicLong(1);
    private final Set<String> knownIds = new HashSet<>();
    private final AtomicBoolean hasWarnedEmpty = new AtomicBoolean(false);

    public WatchLoop(String serviceName) {
      this.serviceName = serviceName;
      this.executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "consul-watch-" + serviceName + "-loop");
        t.setDaemon(true);
        return t;
      });
    }

    public void start() {
      executor.submit(this::run);
    }

    public void stop() {
      running = false;
      executor.shutdownNow();
    }

    private void run() {
      long retryDelayMs = initialRetryDelayMs;

      while (running && !Thread.currentThread().isInterrupted()) {
        try {
          ConsulResponse resp = consulClient.blockingGet("/v1/health/service/" + serviceName, lastIndex.get(), blockSeconds);
          JsonNode response = objectMapper.readTree(resp.body());
          if (response.isEmpty()) {
            if (hasWarnedEmpty.compareAndSet(false, true)) {
              log.warn("Received empty service list from Consul for service '{}'. This may be normal during startup.", serviceName);
            } else {
              log.info("Still waiting for instances of service '{}' to appear in Consul", serviceName);
            }
            handleRetryDelay(retryDelayMs);
            retryDelayMs = Math.min(retryDelayMs * 2, maxRetryDelayMs);
            continue;
          }

          Set<String> currentIds = new HashSet<>();
          for (JsonNode node : response) {
            JsonNode checks = node.get("Checks");
            if (checks == null || checks.isEmpty() || !"passing".equals(checks.get(0).path("Status").asText())) {
              continue;
            }
            JsonNode svc = node.get("Service");
            if (svc == null) continue;

            String id = svc.path("ID").asText();
            String address = svc.path("Address").asText();
            int port = svc.path("Port").asInt();

            currentIds.add(id);
            if (!knownIds.contains(id)) {
              loadBalancer.registerInstance(serviceName, id, address, port);
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

          retryDelayMs = initialRetryDelayMs;
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
  }
}
