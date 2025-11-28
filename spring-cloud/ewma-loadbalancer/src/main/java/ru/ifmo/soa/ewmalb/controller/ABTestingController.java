package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.ABTestingService;

import java.util.Map;

@RestController
@RequestMapping("/ab-testing")
public class ABTestingController {

  private final ABTestingService abTestingService;

  public ABTestingController(ABTestingService abTestingService) {
    this.abTestingService = abTestingService;
  }

  @GetMapping("/experiments")
  public Map<String, Object> getExperiments() {
    return abTestingService.getExperimentStats();
  }

  @PostMapping("/experiments/{experimentId}/assign/{userId}")
  public Map<String, Object> assignUserToVariant(
    @PathVariable String experimentId,
    @PathVariable String userId,
    @RequestParam String variant) {

    return Map.of(
      "experimentId", experimentId,
      "userId", userId,
      "variant", variant,
      "assigned", true,
      "timestamp", System.currentTimeMillis()
    );
  }

  @GetMapping("/experiments/{experimentId}/stats")
  public Map<String, Object> getExperimentStats(@PathVariable String experimentId) {
    Map<String, Object> allStats = abTestingService.getExperimentStats();
    @SuppressWarnings("unchecked")
    Map<String, Object> experiments = (Map<String, Object>) allStats.get("experiments");

    return Map.of(
      "experimentId", experimentId,
      "stats", experiments.getOrDefault(experimentId, Map.of()),
      "timestamp", System.currentTimeMillis()
    );
  }
}
