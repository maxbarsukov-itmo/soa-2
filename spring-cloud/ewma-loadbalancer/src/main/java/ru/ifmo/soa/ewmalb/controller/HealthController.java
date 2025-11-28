package ru.ifmo.soa.ewmalb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.lifecycle.GracefulShutdownManager;

import java.util.Map;

@RestController
public class HealthController {

  @Value("${service.meta.version}")
  private String serviceVersion;

  @Value("${service.meta.type}")
  private String serviceType;

  private final EwmaLoadBalancer loadBalancer;
  private final GracefulShutdownManager shutdownManager;

  public HealthController(EwmaLoadBalancer loadBalancer, GracefulShutdownManager shutdownManager) {
    this.loadBalancer = loadBalancer;
    this.shutdownManager = shutdownManager;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    int healthyInstances = loadBalancer.getHealthyInstanceCount();
    boolean isHealthy = healthyInstances > 0 && !shutdownManager.isShuttingDown();

    return Map.of(
      "status", isHealthy ? "UP" : "DOWN",
      "timestamp", System.currentTimeMillis(),
      "loadBalancer", Map.of(
        "type", serviceType,
        "healthyInstances", healthyInstances,
        "totalInstances", loadBalancer.getInstanceCount(),
        "drain_mode", shutdownManager.isInDrainMode(),
        "shutting_down", shutdownManager.isShuttingDown(),
        "version", serviceVersion
      ),
      "details", isHealthy ? "Load balancer is operational" :
        shutdownManager.isShuttingDown() ? "Load balancer is shutting down" :
          "No healthy backends available"
    );
  }

  @GetMapping("/lb/status")
  public Map<String, Object> detailedStatus() {
    return Map.of(
      "status", "OPERATIONAL",
      "component", "EWMA Load Balancer",
      "instances", Map.of(
        "total", loadBalancer.getInstanceCount(),
        "healthy", loadBalancer.getHealthyInstanceCount(),
        "unhealthy", loadBalancer.getInstanceCount() - loadBalancer.getHealthyInstanceCount()
      ),
      "circuitBreakers", loadBalancer.getCircuitBreakerStats(),
      "timestamp", System.currentTimeMillis()
    );
  }
}
