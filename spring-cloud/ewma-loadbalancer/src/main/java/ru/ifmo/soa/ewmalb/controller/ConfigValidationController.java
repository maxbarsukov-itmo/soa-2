package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.ConfigValidationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/management/validation")
public class ConfigValidationController {

  private final ConfigValidationService validationService;

  public ConfigValidationController(ConfigValidationService validationService) {
    this.validationService = validationService;
  }

  @PostMapping("/run")
  public Map<String, Object> runValidation() {
    List<ConfigValidationService.ValidationResult> results = validationService.validateAll();

    long errorCount = results.stream().mapToLong(r -> r.getErrors().size()).sum();
    long warningCount = results.stream().mapToLong(r -> r.getWarnings().size()).sum();

    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "results", results,
      "summary", Map.of(
        "totalChecks", results.size(),
        "errors", errorCount,
        "warnings", warningCount,
        "healthy", errorCount == 0
      )
    );
  }

  @GetMapping("/stats")
  public Map<String, Object> getValidationStats() {
    return validationService.getValidationStats();
  }

  @PostMapping("/self-healing/{enabled}")
  public Map<String, Object> setSelfHealing(@PathVariable boolean enabled) {
    validationService.setSelfHealingEnabled(enabled);

    return Map.of(
      "selfHealingEnabled", enabled,
      "timestamp", System.currentTimeMillis(),
      "message", "Self-healing " + (enabled ? "enabled" : "disabled")
    );
  }

  @GetMapping("/history")
  public Map<String, Object> getValidationHistory() {
    return validationService.getValidationStats();
  }
}
