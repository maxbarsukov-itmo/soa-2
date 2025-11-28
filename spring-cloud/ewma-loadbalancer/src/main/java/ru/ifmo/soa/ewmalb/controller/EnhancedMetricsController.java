package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.LatencyDistributionService;

import java.util.Map;

@RestController
@RequestMapping("/metrics/enhanced")
public class EnhancedMetricsController {

  private final LatencyDistributionService latencyDistributionService;

  public EnhancedMetricsController(LatencyDistributionService latencyDistributionService) {
    this.latencyDistributionService = latencyDistributionService;
  }

  @GetMapping("/latency/distribution")
  public Map<String, Object> getLatencyDistributions() {
    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "distributions", latencyDistributionService.getAllDistributions()
    );
  }

  @GetMapping("/latency/distribution/{instanceId}")
  public Map<String, Object> getInstanceLatencyDistribution(@PathVariable String instanceId) {
    return Map.of(
      "instance_id", instanceId,
      "timestamp", System.currentTimeMillis(),
      "distribution", latencyDistributionService.getLatencyDistribution(instanceId),
      "percentiles", latencyDistributionService.getPercentiles(instanceId)
    );
  }

  @PostMapping("/latency/distribution/{instanceId}/reset")
  public Map<String, Object> resetLatencyDistribution(@PathVariable String instanceId) {
    latencyDistributionService.resetDistribution(instanceId);
    return Map.of(
      "instance_id", instanceId,
      "action", "distribution_reset",
      "timestamp", System.currentTimeMillis()
    );
  }
}
