package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.RateLimitService;

import java.util.Map;

@RestController
@RequestMapping("/management/rate-limit")
public class RateLimitController {

  private final RateLimitService rateLimitService;

  public RateLimitController(RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
  }

  @GetMapping("/stats/{clientId}")
  public Map<String, Object> getRateLimitStats(@PathVariable String clientId) {
    return Map.of(
      "clientId", clientId,
      "timestamp", System.currentTimeMillis(),
      "rateLimits", rateLimitService.getRateLimitStats(clientId)
    );
  }

  @PostMapping("/reset/{bucketType}/{identifier}")
  public Map<String, Object> resetRateLimit(
    @PathVariable String bucketType,
    @PathVariable String identifier) {

    rateLimitService.resetBucket(bucketType, identifier);

    return Map.of(
      "bucketType", bucketType,
      "identifier", identifier,
      "action", "reset",
      "timestamp", System.currentTimeMillis(),
      "message", "Rate limit bucket reset successfully"
    );
  }

  @GetMapping("/global-stats")
  public Map<String, Object> getGlobalStats() {
    return rateLimitService.getRateLimitStats(null);
  }
}
