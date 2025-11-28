package ru.ifmo.soa.ewmalb.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.metrics.FallbackMeterRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EnhancedHealthCheckService {

  private static final Logger log = LoggerFactory.getLogger(EnhancedHealthCheckService.class);

  private final EwmaLoadBalancer loadBalancer;
  private final CloseableHttpClient httpClient;
  private final MeterRegistry meterRegistry;
  private final ConcurrentHashMap<String, Timer> healthCheckTimers;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private Thread healthCheckThread;

  private final AtomicInteger totalHealthChecks = new AtomicInteger(0);
  private final AtomicInteger failedHealthChecks = new AtomicInteger(0);
  private final boolean metricsEnabled;

  private final String healthCheckPath;
  private final long healthCheckInterval;
  private final int healthCheckTimeout;

  @Autowired
  public EnhancedHealthCheckService(EwmaLoadBalancer loadBalancer,
                                    MeterRegistry meterRegistry,
                                    @Value("${ewma.loadbalancer.health-check.path:/health}") String healthCheckPath,
                                    @Value("${ewma.loadbalancer.health-check.interval-ms:30000}") long healthCheckInterval,
                                    @Value("${ewma.loadbalancer.health-check.timeout-ms:5000}") int healthCheckTimeout) {
    this.loadBalancer = loadBalancer;
    this.meterRegistry = meterRegistry;
    this.healthCheckPath = healthCheckPath;
    this.healthCheckInterval = healthCheckInterval;
    this.healthCheckTimeout = healthCheckTimeout;
    this.httpClient = HttpClients.createDefault();
    this.healthCheckTimers = new ConcurrentHashMap<>();

    this.metricsEnabled = !(meterRegistry instanceof FallbackMeterRegistry);

    initMetrics();
  }

  private void initMetrics() {
    if (!metricsEnabled) {
      log.info("Metrics are disabled, using fallback MeterRegistry");
      return;
    }

    try {
      meterRegistry.gauge("loadbalancer.healthchecks.total", totalHealthChecks);
      meterRegistry.gauge("loadbalancer.healthchecks.failed", failedHealthChecks);
      meterRegistry.gauge("loadbalancer.healthchecks.success_rate",
        totalHealthChecks, total -> {
          int totalCount = total.get();
          return totalCount > 0 ?
            (double) (totalCount - failedHealthChecks.get()) / totalCount * 100 : 100.0;
        });
      log.debug("Health check metrics initialized with path: {}, interval: {}ms, timeout: {}ms",
        healthCheckPath, healthCheckInterval, healthCheckTimeout);
    } catch (Exception e) {
      log.warn("Failed to initialize health check metrics: {}", e.getMessage());
    }
  }

  @PostConstruct
  public void startHealthChecks() {
    log.info("Starting enhanced health checks (path: {}, interval: {}ms, timeout: {}ms, metrics enabled: {})",
      healthCheckPath, healthCheckInterval, healthCheckTimeout, metricsEnabled);

    healthCheckThread = new Thread(this::healthCheckLoop, "enhanced-health-check-worker");
    healthCheckThread.setDaemon(true);
    healthCheckThread.start();
  }

  private void healthCheckLoop() {
    while (!Thread.currentThread().isInterrupted() && running.get()) {
      try {
        performEnhancedHealthChecks();
        Thread.sleep(healthCheckInterval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        log.error("Error in health check loop: {}", e.getMessage());
      }
    }
  }

  public void performEnhancedHealthChecks() {
    loadBalancer.getAllInstances().forEach(instance -> {
      String healthUrl = instance.getUrl() + healthCheckPath;
      totalHealthChecks.incrementAndGet();

      Timer.Sample sample = Timer.start(meterRegistry);
      boolean success = false;

      try {
        HttpGet request = new HttpGet(healthUrl);
        request.setHeader("User-Agent", "EWMA-LoadBalancer/1.0");
        request.setHeader("X-Health-Check", "true");

        RequestConfig requestConfig = RequestConfig.custom()
          .setConnectionRequestTimeout(Timeout.ofMilliseconds(healthCheckTimeout))
          .setResponseTimeout(Timeout.ofMilliseconds(healthCheckTimeout))
          .build();
        request.setConfig(requestConfig);

        success = httpClient.execute(request, response -> {
          int statusCode = response.getCode();
          boolean isHealthy = statusCode >= 200 && statusCode < 300;

          if (isHealthy) {
            instance.recordSuccess();
            log.debug("Health check passed for {}: HTTP {}",
              instance.getId(), statusCode);
          } else {
            instance.recordFailure();
            failedHealthChecks.incrementAndGet();
            log.warn("Health check failed for {}: HTTP {}",
              instance.getId(), statusCode);
          }

          instance.setHealthy(isHealthy);
          recordHealthCheckMetrics(instance.getId(), statusCode, true);
          return isHealthy;
        });

      } catch (Exception e) {
        log.warn("Health check error for {}: {}", instance.getId(), e.getMessage());
        instance.recordFailure();
        instance.setHealthy(false);
        failedHealthChecks.incrementAndGet();
        recordHealthCheckMetrics(instance.getId(), 0, false);
      } finally {
        if (getHealthCheckTimer(instance.getId()) != null) {
          sample.stop(getHealthCheckTimer(instance.getId()));
        }
      }
    });

    exportRealTimeMetrics();
  }

  @PreDestroy
  public void stopHealthChecks() {
    log.info("Stopping enhanced health checks...");
    running.set(false);

    if (healthCheckThread != null && healthCheckThread.isAlive()) {
      healthCheckThread.interrupt();
      try {
        healthCheckThread.join(5000);
        log.debug("Enhanced health check thread stopped");
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for health check thread to stop");
        Thread.currentThread().interrupt();
      }
    }

    try {
      httpClient.close();
      log.debug("Enhanced health check HTTP client closed");
    } catch (Exception e) {
      log.warn("Error closing enhanced health check HTTP client: {}", e.getMessage());
    }
  }

  private Timer getHealthCheckTimer(String instanceId) {
    if (!metricsEnabled) {
      return null;
    }

    return healthCheckTimers.computeIfAbsent(instanceId, id -> {
      try {
        return Timer.builder("loadbalancer.healthcheck.duration")
          .tag("instance", instanceId)
          .register(meterRegistry);
      } catch (Exception e) {
        log.warn("Failed to create timer for instance {}: {}", instanceId, e.getMessage());
        return null;
      }
    });
  }

  private void recordHealthCheckMetrics(String instanceId, int statusCode, boolean successful) {
    if (!metricsEnabled || !running.get()) return;

    try {
      meterRegistry.counter("loadbalancer.healthcheck.result",
          "instance", instanceId,
          "status", successful ? "success" : "failure",
          "status_code", String.valueOf(statusCode))
        .increment();
    } catch (Exception e) {
      log.debug("Failed to record health check metrics: {}", e.getMessage());
    }
  }

  private void exportRealTimeMetrics() {
    if (!metricsEnabled || !running.get()) return;

    try {
      loadBalancer.getAllInstances().forEach(instance -> {
        meterRegistry.gauge("loadbalancer.instance.health_status",
          instance, inst -> inst.isHealthy() ? 1 : 0);

        meterRegistry.gauge("loadbalancer.instance.latency_ewma",
          instance, EwmaInstance::getEwmaLatencyMs);

        meterRegistry.gauge("loadbalancer.instance.active_requests",
          instance, inst -> (double) inst.getActiveRequests());

        meterRegistry.gauge("loadbalancer.instance.circuit_breaker_state",
          instance, inst -> inst.getCircuitBreaker().getState().ordinal());
      });
    } catch (Exception e) {
      log.debug("Failed to export real-time metrics: {}", e.getMessage());
    }
  }

  public void performImmediateHealthCheck(String instanceId) {
    if (!running.get()) {
      log.warn("Health checks are disabled, cannot perform immediate check for {}", instanceId);
      return;
    }

    loadBalancer.getAllInstances().stream()
      .filter(inst -> inst.getId().equals(instanceId))
      .findFirst()
      .ifPresent(instance -> {
        new Thread(this::performEnhancedHealthChecks,
          "immediate-healthcheck-" + instanceId).start();
      });
  }

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public boolean isRunning() {
    return running.get();
  }

  public void setRunning(boolean running) {
    this.running.set(running);
  }
}
