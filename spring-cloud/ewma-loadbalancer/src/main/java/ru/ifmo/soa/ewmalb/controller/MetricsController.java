package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.soa.ewmalb.service.MetricsService;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

  private final MetricsService metricsService;

  public MetricsController(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GetMapping
  public Map<String, Object> getMetrics() {
    return metricsService.getMetrics();
  }

  @GetMapping("/summary")
  public Map<String, Object> getMetricsSummary() {
    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "totalRequests", metricsService.getTotalRequests(),
      "successfulRequests", metricsService.getSuccessfulRequests(),
      "failedRequests", metricsService.getFailedRequests(),
      "successRate", metricsService.getSuccessRate(),
      "averageLatencyMs", Math.round(metricsService.getAverageLatency() * 100.0) / 100.0
    );
  }
}
