package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.LoadBalancingStrategyService;

import java.util.Map;

@RestController
@RequestMapping("/management/strategies")
public class LoadBalancingStrategyController {

  private final LoadBalancingStrategyService strategyService;

  public LoadBalancingStrategyController(LoadBalancingStrategyService strategyService) {
    this.strategyService = strategyService;
  }

  @GetMapping("/current")
  public Map<String, Object> getCurrentStrategy() {
    return strategyService.getStrategyStats();
  }

  @PostMapping("/high-latency-mode")
  public Map<String, Object> enableHighLatencyMode() {
    strategyService.adjustLoadBalancingForHighLatency();

    return Map.of(
      "action", "high_latency_mode_enabled",
      "timestamp", System.currentTimeMillis(),
      "message", "Switched to low sensitivity strategy for high latency conditions"
    );
  }

  @PostMapping("/normal-mode")
  public Map<String, Object> enableNormalMode() {
    strategyService.restoreNormalLoadBalancing();

    return Map.of(
      "action", "normal_mode_enabled",
      "timestamp", System.currentTimeMillis(),
      "message", "Restored normal load balancing strategy"
    );
  }

  @PostMapping("/set/{strategy}")
  public Map<String, Object> setStrategy(@PathVariable String strategy) {
    strategyService.setStrategy(strategy.toUpperCase());

    return Map.of(
      "strategy", strategy.toUpperCase(),
      "timestamp", System.currentTimeMillis(),
      "message", "Load balancing strategy updated"
    );
  }
}
