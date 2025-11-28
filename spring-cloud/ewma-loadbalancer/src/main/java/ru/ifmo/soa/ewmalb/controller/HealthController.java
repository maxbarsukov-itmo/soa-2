package ru.ifmo.soa.ewmalb.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.lifecycle.GracefulShutdownManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class HealthController {

  @Value("${ewma.meta.version}")
  private String serviceVersion;

  @Value("${ewma.meta.type}")
  private String serviceType;

  private final EwmaLoadBalancer loadBalancer;
  private final GracefulShutdownManager shutdownManager;

  public HealthController(EwmaLoadBalancer loadBalancer, GracefulShutdownManager shutdownManager) {
    this.loadBalancer = loadBalancer;
    this.shutdownManager = shutdownManager;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    boolean globalHealthy = loadBalancer.getHealthyInstanceCount() > 0 && !shutdownManager.isShuttingDown();

    Map<String, List<EwmaInstance>> byService = loadBalancer.getAllInstances().stream()
      .collect(Collectors.groupingBy(EwmaInstance::getServiceName));

    Map<String, Object> servicesHealth = new LinkedHashMap<>();
    boolean allServicesHealthy = true;

    for (Map.Entry<String, List<EwmaInstance>> entry : byService.entrySet()) {
      String serviceName = entry.getKey();
      List<EwmaInstance> instances = entry.getValue();
      long healthyCount = instances.stream().filter(EwmaInstance::isHealthy).count();
      boolean serviceHealthy = healthyCount > 0;
      if (!serviceHealthy) allServicesHealthy = false;

      servicesHealth.put(serviceName, Map.of(
        "total", instances.size(),
        "healthy", healthyCount,
        "unhealthy", instances.size() - healthyCount,
        "status", serviceHealthy ? "UP" : "DOWN"
      ));
    }

    return Map.of(
      "status", (globalHealthy && allServicesHealthy) ? "UP" : "DOWN",
      "timestamp", System.currentTimeMillis(),
      "loadBalancer", Map.of(
        "type", "EWMA",
        "totalInstances", loadBalancer.getInstanceCount(),
        "healthyInstances", loadBalancer.getHealthyInstanceCount(),
        "drain_mode", shutdownManager.isInDrainMode(),
        "shutting_down", shutdownManager.isShuttingDown()
      ),
      "services", servicesHealth,
      "details", (globalHealthy && allServicesHealthy)
        ? "All backend services are operational"
        : "One or more backend services are DOWN"
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
