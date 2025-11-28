package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.config.LoadBalancerConfig;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;

@Service
public class LoadBalancingStrategyService {

  @Value("${ewma.strategies.high-latency-mode.duration-ms:300000}")
  private long highLatencyModeDuration;

  @Value("${ewma.strategies.high-latency-mode.avg-latency-threshold-ms:1000}")
  private double avgLatencyThreshold;

  @Value("${ewma.strategies.high-latency-mode.std-dev-threshold-ms:500}")
  private double stdDevThreshold;

  @Value("${ewma.strategies.high-latency-mode.restore-latency-threshold-ms:200}")
  private double restoreLatencyThreshold;

  @Value("${ewma.strategies.high-latency-mode.restore-std-dev-threshold-ms:100}")
  private double restoreStdDevThreshold;

  @Value("${ewma.strategies.high-latency-mode.enabled:true}")
  private boolean highLatencyModeEnabled;

  @Value("${ewma.strategies.high-latency-mode.healthy-ratio-for-least-connections:0.5}")
  private double healthyRatioForLeastConnections;

  private static final Logger log = LoggerFactory.getLogger(LoadBalancingStrategyService.class);

  private final EwmaLoadBalancer loadBalancer;
  private final LoadBalancerConfig config;

  private final AtomicReference<String> currentStrategy = new AtomicReference<>();
  private final AtomicReference<String> originalStrategy = new AtomicReference<>();
  private final ReentrantLock strategyLock = new ReentrantLock();
  private volatile long highLatencyModeStartTime = 0;

  public LoadBalancingStrategyService(EwmaLoadBalancer loadBalancer, LoadBalancerConfig config) {
    this.loadBalancer = loadBalancer;
    this.config = config;
  }

  @PostConstruct
  public void init() {
    originalStrategy.set(config.getSelection().getStrategy());
    currentStrategy.set(originalStrategy.get());
    log.info("Load balancing strategy service initialized. Initial strategy: {}", currentStrategy.get());
  }

  public void adjustLoadBalancingForHighLatency() {
    strategyLock.lock();
    try {
      if (highLatencyModeEnabled) {
        log.debug("Already in high latency mode, extending duration");
        highLatencyModeStartTime = System.currentTimeMillis();
        return;
      }

      log.warn("Entering high latency mode - switching to low sensitivity strategy");

      originalStrategy.set(currentStrategy.get());

      String lowSensitivityStrategy = selectLowSensitivityStrategy();
      currentStrategy.set(lowSensitivityStrategy);

      highLatencyModeEnabled = true;
      highLatencyModeStartTime = System.currentTimeMillis();

      applyStrategyToBalancer(lowSensitivityStrategy);

      log.info("Switched to low sensitivity strategy: {} (was: {})",
        lowSensitivityStrategy, originalStrategy.get());

    } finally {
      strategyLock.unlock();
    }
  }

  public void restoreNormalLoadBalancing() {
    strategyLock.lock();
    try {
      if (!highLatencyModeEnabled) {
        return;
      }

      log.info("Restoring normal load balancing strategy");

      String original = originalStrategy.get();
      currentStrategy.set(original);
      highLatencyModeEnabled = false;

      applyStrategyToBalancer(original);

      log.info("Restored original strategy: {}", original);

    } finally {
      strategyLock.unlock();
    }
  }

  public void checkAndRestoreStrategy() {
    if (highLatencyModeEnabled &&
      System.currentTimeMillis() - highLatencyModeStartTime > highLatencyModeDuration) {
      restoreNormalLoadBalancing();
    }
  }

  private String selectLowSensitivityStrategy() {
    int totalInstances = loadBalancer.getInstanceCount();
    int healthyInstances = loadBalancer.getHealthyInstanceCount();

    if (healthyInstances == 0) {
      return "WEIGHTED_RANDOM";
    }

    if ((double) healthyInstances / totalInstances < healthyRatioForLeastConnections) {
      return "LEAST_CONNECTIONS";
    }

    return "WEIGHTED_RANDOM";
  }

  private void applyStrategyToBalancer(String strategy) {
    loadBalancer.setSelectionStrategy(strategy);
    log.debug("Strategy applied to load balancer: {}", strategy);
  }

  public void adjustStrategyBasedOnMetrics() {
    double avgLatency = calculateAverageLatency();
    double latencyStdDev = calculateLatencyStdDev();

    if (avgLatency > avgLatencyThreshold || latencyStdDev > stdDevThreshold) {
      if (!highLatencyModeEnabled) {
        adjustLoadBalancingForHighLatency();
      }
    } else if (highLatencyModeEnabled && avgLatency < restoreLatencyThreshold && latencyStdDev < restoreStdDevThreshold) {
      restoreNormalLoadBalancing();
    }
  }

  private double calculateAverageLatency() {
    return loadBalancer.getAllInstances().stream()
      .filter(EwmaInstance::isHealthy)
      .mapToDouble(EwmaInstance::getEwmaLatencyMs)
      .average()
      .orElse(0.0);
  }

  private double calculateLatencyStdDev() {
    double avg = calculateAverageLatency();
    if (avg == 0) return 0;

    double variance = loadBalancer.getAllInstances().stream()
      .filter(EwmaInstance::isHealthy)
      .mapToDouble(instance -> Math.pow(instance.getEwmaLatencyMs() - avg, 2))
      .average()
      .orElse(0.0);

    return Math.sqrt(variance);
  }

  public Map<String, Object> getStrategyStats() {
    return Map.of(
      "currentStrategy", currentStrategy.get(),
      "originalStrategy", originalStrategy.get(),
      "highLatencyModeEnabled", highLatencyModeEnabled,
      "highLatencyModeStartTime", highLatencyModeStartTime,
      "highLatencyModeDuration", highLatencyModeDuration,
      "timeInHighLatencyMode", highLatencyModeEnabled ?
        System.currentTimeMillis() - highLatencyModeStartTime : 0,
      "averageLatency", calculateAverageLatency(),
      "latencyStdDev", calculateLatencyStdDev()
    );
  }

  public void setStrategy(String strategy) {
    strategyLock.lock();
    try {
      if (highLatencyModeEnabled) {
        originalStrategy.set(strategy);
      } else {
        currentStrategy.set(strategy);
        applyStrategyToBalancer(strategy);
      }
      log.info("Strategy set to: {}", strategy);
    } finally {
      strategyLock.unlock();
    }
  }
}
