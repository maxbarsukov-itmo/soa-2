package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.service.MetricsService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metrics/export")
public class MetricsExportController {

  private final EwmaLoadBalancer loadBalancer;
  private final MetricsService metricsService;

  public MetricsExportController(EwmaLoadBalancer loadBalancer,
                                 MetricsService metricsService) {
    this.loadBalancer = loadBalancer;
    this.metricsService = metricsService;
  }

  @GetMapping("/prometheus")
  public String exportPrometheusMetrics() {
    StringBuilder sb = new StringBuilder();

    sb.append("# HELP loadbalancer_healthchecks_total Total health checks performed\n");
    sb.append("# TYPE loadbalancer_healthchecks_total counter\n");
    sb.append("loadbalancer_healthchecks_total ")
      .append(metricsService.getTotalRequests()).append("\n");

    loadBalancer.getAllInstances().forEach(instance -> {
      String instanceId = sanitizeForPrometheus(instance.getId());

      sb.append("# HELP loadbalancer_instance_info Instance information\n");
      sb.append("# TYPE loadbalancer_instance_info gauge\n");
      sb.append(String.format("loadbalancer_instance_info{instance=\"%s\",address=\"%s\"} 1\n",
        instanceId, instance.getAddress()));

      sb.append("# HELP loadbalancer_instance_healthy Instance health status\n");
      sb.append("# TYPE loadbalancer_instance_healthy gauge\n");
      sb.append(String.format("loadbalancer_instance_healthy{instance=\"%s\"} %d\n",
        instanceId, instance.isHealthy() ? 1 : 0));

      sb.append("# HELP loadbalancer_instance_latency_ewma EWMA latency in milliseconds\n");
      sb.append("# TYPE loadbalancer_instance_latency_ewma gauge\n");
      sb.append(String.format("loadbalancer_instance_latency_ewma{instance=\"%s\"} %.2f\n",
        instanceId, instance.getEwmaLatencyMs()));

      sb.append("# HELP loadbalancer_instance_active_requests Active requests count\n");
      sb.append("# TYPE loadbalancer_instance_active_requests gauge\n");
      sb.append(String.format("loadbalancer_instance_active_requests{instance=\"%s\"} %d\n",
        instanceId, instance.getActiveRequests()));
    });

    return sb.toString();
  }

  @GetMapping("/json")
  public Map<String, Object> exportJsonMetrics() {
    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("timestamp", System.currentTimeMillis());
    metrics.put("format", "real-time");

    Map<String, Object> loadMetrics = new HashMap<>();
    loadMetrics.put("total_instances", loadBalancer.getInstanceCount());
    loadMetrics.put("healthy_instances", loadBalancer.getHealthyInstanceCount());
    loadMetrics.put("unhealthy_instances",
      loadBalancer.getInstanceCount() - loadBalancer.getHealthyInstanceCount());

    metrics.put("load", loadMetrics);

    List<Map<String, Object>> instanceMetrics = loadBalancer.getAllInstances().stream()
      .map(instance -> {
        Map<String, Object> instanceData = new HashMap<>();
        instanceData.put("id", instance.getId());
        instanceData.put("url", instance.getUrl());
        instanceData.put("healthy", instance.isHealthy());
        instanceData.put("ewma_latency_ms", Math.round(instance.getEwmaLatencyMs() * 100.0) / 100.0);
        instanceData.put("active_requests", instance.getActiveRequests());
        instanceData.put("total_requests", instance.getTotalRequests());
        instanceData.put("circuit_breaker_state", instance.getCircuitBreaker().getState().name());
        instanceData.put("failure_count", instance.getCircuitBreaker().getFailureCount());
        instanceData.put("last_response_time", instance.getLastResponse());

        return instanceData;
      })
      .collect(Collectors.toList());

    metrics.put("instances", instanceMetrics);

    Map<String, Object> systemMetrics = new HashMap<>();
    systemMetrics.put("available_processors", Runtime.getRuntime().availableProcessors());
    systemMetrics.put("free_memory_mb", Runtime.getRuntime().freeMemory() / 1024 / 1024);
    systemMetrics.put("total_memory_mb", Runtime.getRuntime().totalMemory() / 1024 / 1024);
    systemMetrics.put("max_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024);

    metrics.put("system", systemMetrics);

    return metrics;
  }

  private String sanitizeForPrometheus(String input) {
    return input.replaceAll("[^a-zA-Z0-9_]", "_");
  }
}
