package ru.ifmo.soa.ewmalb.balancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.ifmo.soa.ewmalb.service.AdaptiveEwmaService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class EwmaLoadBalancer {

  private static final Logger log = LoggerFactory.getLogger(EwmaLoadBalancer.class);

  private final ConcurrentHashMap<String, EwmaInstance> instances = new ConcurrentHashMap<>();
  private final AdaptiveEwmaService adaptiveEwmaService;

  private volatile boolean drainMode = false;

  @Value("${ewma.loadbalancer.selection.strategy}")
  private String selectionStrategy;

  @Value("${ewma.loadbalancer.circuit-breaker.failure-threshold:5}")
  private int circuitBreakerFailureThreshold;

  @Value("${ewma.loadbalancer.circuit-breaker.reset-timeout-ms:60000}")
  private long circuitBreakerResetTimeoutMs;

  @Value("${ewma.loadbalancer.load-aware.max-concurrent-requests:100}")
  private int maxConcurrentRequests;

  @Value("${ewma.loadbalancer.graceful-degradation.allow-unhealthy}")
  private boolean allowUnhealthyInstances;

  @Value("${ewma.loadbalancer.circuit-breaker.half-open-timeout-ms:30000}")
  private long halfOpenTimeoutMs;

  public EwmaLoadBalancer(AdaptiveEwmaService adaptiveEwmaService) {
    this.adaptiveEwmaService = adaptiveEwmaService;
  }

  public void registerInstance(String serviceName, String id, String address, int port) {
    EwmaInstance instance = new EwmaInstance(
      serviceName, id, address, port,
      circuitBreakerFailureThreshold,
      circuitBreakerResetTimeoutMs,
      halfOpenTimeoutMs
    );
    instances.put(id, instance);
    log.info("Registered backend: {} (service: {}) - {}:{}", id, serviceName, address, port);
  }

  public void unregisterInstance(String id) {
    if (instances.remove(id) != null) {
      log.info("Unregistered backend: {}", id);
    }
  }

  public void recordLatency(String id, long latencyMs) {
    EwmaInstance inst = instances.get(id);
    if (inst != null) {
      double alpha = adaptiveEwmaService.calculateAlpha(inst, latencyMs);
      inst.updateLatencyWithAlpha(latencyMs, alpha);
      inst.recordSuccess();
      if (alpha != 0.2) {
        log.debug("Adaptive alpha for {}: {} (latency: {}ms)", id, String.format("%.3f", alpha), latencyMs);
      }
    }
  }

  public EwmaInstance selectInstanceForService(String serviceName) {
    List<EwmaInstance> candidates = getCandidatesForService(serviceName);
    if (candidates.isEmpty()) {
      return gracefulFallbackForService(serviceName);
    }
    return switch (selectionStrategy.toUpperCase()) {
      case "WEIGHTED_RANDOM" -> selectWeightedRandom(candidates);
      case "LOAD_AWARE" -> selectLoadAware(candidates);
      case "PURE_EWMA" -> selectPureEwma(candidates);
      case "LEAST_CONNECTIONS" -> selectLeastConnections(candidates);
      default -> selectWeightedRandom(candidates);
    };
  }

  private List<EwmaInstance> getCandidatesForService(String serviceName) {
    if (drainMode) {
      log.debug("Load balancer is in DRAIN mode - rejecting new requests");
      return Collections.emptyList();
    }
    List<EwmaInstance> healthyInstances = instances.values().stream()
      .filter(inst -> inst.getServiceName().equals(serviceName) && inst.isHealthy())
      .collect(Collectors.toList());
    if (healthyInstances.isEmpty() && allowUnhealthyInstances) {
      log.warn("No healthy instances for {}, falling back to unhealthy", serviceName);
      return instances.values().stream()
        .filter(inst -> inst.getServiceName().equals(serviceName) &&
          inst.getCircuitBreaker().getState() == CircuitBreaker.State.HALF_OPEN)
        .collect(Collectors.toList());
    }
    return healthyInstances;
  }

  private EwmaInstance gracefulFallbackForService(String serviceName) {
    if (allowUnhealthyInstances) {
      return instances.values().stream()
        .filter(inst -> inst.getServiceName().equals(serviceName))
        .min(Comparator.comparingInt(inst -> inst.getCircuitBreaker().getFailureCount()))
        .orElse(null);
    }
    log.error("No instances available for service: {}", serviceName);
    return null;
  }

  private EwmaInstance selectWeightedRandom(List<EwmaInstance> candidates) {
    double totalWeight = candidates.stream()
      .mapToDouble(this::calculateInstanceWeight)
      .sum();
    double random = ThreadLocalRandom.current().nextDouble(totalWeight);
    double current = 0;
    for (EwmaInstance instance : candidates) {
      current += calculateInstanceWeight(instance);
      if (random <= current) {
        return instance;
      }
    }
    return candidates.get(0);
  }

  private EwmaInstance selectLoadAware(List<EwmaInstance> candidates) {
    List<EwmaInstance> loadAware = candidates.stream()
      .filter(inst -> inst.getActiveRequests() < maxConcurrentRequests)
      .collect(Collectors.toList());
    if (loadAware.isEmpty()) loadAware = candidates;
    return loadAware.stream()
      .min(Comparator.comparingDouble(inst ->
        inst.getPredictedLatency() * (1 + inst.getActiveRequests() * 0.01)
      ))
      .orElse(null);
  }

  private EwmaInstance selectPureEwma(List<EwmaInstance> candidates) {
    return candidates.stream()
      .min(Comparator.comparingDouble(EwmaInstance::getEwmaLatencyMs))
      .orElse(null);
  }

  private EwmaInstance selectLeastConnections(List<EwmaInstance> candidates) {
    return candidates.stream()
      .min(Comparator.comparingInt(EwmaInstance::getActiveRequests))
      .orElse(null);
  }

  private double calculateInstanceWeight(EwmaInstance instance) {
    double latency = instance.getPredictedLatency();
    return 1.0 / (latency + 1.0);
  }

  public List<EwmaInstance> getInstancesForService(String serviceName) {
    return instances.values().stream()
      .filter(inst -> inst.getServiceName().equals(serviceName))
      .collect(Collectors.toList());
  }

  public void recordFailure(String id) {
    EwmaInstance inst = instances.get(id);
    if (inst != null) {
      inst.recordFailure();
    }
  }

  public void setDrainMode(boolean drainMode) {
    this.drainMode = drainMode;
    log.info("Load balancer drain mode set to: {}", drainMode);
  }

  public List<EwmaInstance> getAllInstances() {
    return new ArrayList<>(instances.values());
  }

  public int getInstanceCount() {
    return instances.size();
  }

  public int getHealthyInstanceCount() {
    return (int) instances.values().stream()
      .filter(EwmaInstance::isHealthy)
      .count();
  }

  public String getSelectionStrategy() {
    return selectionStrategy;
  }

  public void setSelectionStrategy(String selectionStrategy) {
    this.selectionStrategy = selectionStrategy;
  }

  public int getCircuitBreakerFailureThreshold() {
    return circuitBreakerFailureThreshold;
  }

  public long getCircuitBreakerResetTimeoutMs() {
    return circuitBreakerResetTimeoutMs;
  }

  public boolean setInstanceHealth(String id, boolean healthy) {
    EwmaInstance instance = instances.get(id);
    if (instance != null) {
      instance.setHealthy(healthy);
      return true;
    }
    return false;
  }

  public Map<String, Object> getStats() {
    return Map.of(
      "totalInstances", getInstanceCount(),
      "healthyInstances", getHealthyInstanceCount(),
      "selectionStrategy", selectionStrategy,
      "instances", instances.values().stream()
        .collect(Collectors.toMap(
          EwmaInstance::getId,
          inst -> Map.of(
            "latency", String.format("%.2fms", inst.getEwmaLatencyMs()),
            "healthy", inst.isHealthy(),
            "activeRequests", inst.getActiveRequests(),
            "circuitBreaker", inst.getCircuitBreaker().getState().name()
          )
        ))
    );
  }

  public Map<String, Object> getCircuitBreakerStats() {
    return instances.values().stream()
      .collect(Collectors.toMap(
        EwmaInstance::getId,
        inst -> Map.of(
          "state", inst.getCircuitBreaker().getState().name(),
          "failures", inst.getCircuitBreaker().getFailureCount(),
          "healthy", inst.isHealthy()
        )
      ));
  }

  public Map<String, Object> getSelectionStats() {
    return Map.of(
      "strategy", selectionStrategy,
      "totalInstances", getInstanceCount(),
      "healthyInstances", getHealthyInstanceCount(),
      "allowUnhealthyFallback", allowUnhealthyInstances,
      "maxConcurrentRequests", maxConcurrentRequests
    );
  }

  public void logCurrentState() {
    log.info("=== EWMA Load Balancer State ===");
    if (instances.isEmpty()) {
      log.info("No backends registered");
    } else {
      instances.values().forEach(inst ->
        log.info("{} | lat: {}ms | healthy: {} | circuit: {} | failures: {}",
          inst.getId(),
          String.format("%.2f", inst.getEwmaLatencyMs()),
          inst.isHealthy(),
          inst.getCircuitBreaker().getState(),
          inst.getCircuitBreaker().getFailureCount()
        ));
    }
  }
}
