package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.service.MetricsService;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {

  private final EwmaLoadBalancer loadBalancer;
  private final MetricsService metricsService;

  public DebugController(EwmaLoadBalancer loadBalancer, MetricsService metricsService) {
    this.loadBalancer = loadBalancer;
    this.metricsService = metricsService;
  }

  @GetMapping("/instances")
  public Map<String, Object> getInstances() {
    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "loadBalancer", Map.of(
        "type", "EWMA",
        "totalInstances", loadBalancer.getInstanceCount(),
        "healthyInstances", loadBalancer.getHealthyInstanceCount()
      ),
      "instances", loadBalancer.getAllInstances().stream()
        .map(instance -> Map.of(
          "id", instance.getId(),
          "url", instance.getUrl(),
          "ewmaLatencyMs", String.format("%.2f", instance.getEwmaLatencyMs()),
          "averageLatencyMs", String.format("%.2f", instance.getAverageLatency()),
          "healthy", instance.isHealthy(),
          "activeRequests", instance.getActiveRequests(),
          "totalRequests", instance.getTotalRequests(),
          "circuitBreaker", Map.of(
            "state", instance.getCircuitBreaker().getState().name(),
            "failures", instance.getCircuitBreaker().getFailureCount()
          ),
          "lastResponse", instance.getLastResponse()
        ))
        .collect(Collectors.toList())
    );
  }

  @GetMapping("/tree")
  public Map<String, Object> getLoadBalancerTree() {
    return Map.of(
      "root", Map.of(
        "type", "EWMA Load Balancer",
        "service", "ewma-loadbalancer",
        "children", loadBalancer.getAllInstances().stream()
          .map(instance -> Map.of(
            "type", "Backend Service",
            "id", instance.getId(),
            "url", instance.getUrl(),
            "status", instance.isHealthy() ? "HEALTHY" : "UNHEALTHY",
            "latency", String.format("%.2fms", instance.getEwmaLatencyMs())
          ))
          .collect(Collectors.toList())
      )
    );
  }

  @GetMapping("/metrics/detailed")
  public Map<String, Object> getDetailedMetrics() {
    return metricsService.getMetrics();
  }

  @GetMapping("/retry/config")
  public Map<String, Object> getRetryConfig() {
    return Map.of(
      "enabled", true,
      "maxAttempts", 3,
      "baseBackoffMs", 100
    );
  }

  @GetMapping("/health/stats")
  public Map<String, Object> getHealthStats() {
    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "loadBalancer", "HEALTHY",
      "backendServices", Map.of(
        "total", loadBalancer.getInstanceCount(),
        "healthy", loadBalancer.getHealthyInstanceCount(),
        "unhealthy", loadBalancer.getInstanceCount() - loadBalancer.getHealthyInstanceCount()
      ),
      "metrics", Map.of(
        "totalRequests", metricsService.getTotalRequests(),
        "successRate", String.format("%.2f%%", metricsService.getSuccessRate()),
        "averageLatency", String.format("%.2fms", metricsService.getAverageLatency())
      )
    );
  }
}
