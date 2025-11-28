package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.CircuitBreaker;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.config.ABTestingConfig;
import ru.ifmo.soa.ewmalb.config.LoadBalancerConfig;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConfigValidationService {

  private static final Logger log = LoggerFactory.getLogger(ConfigValidationService.class);

  @Value("${ewma.monitoring.validation.interval-ms:60000}")
  private long validationIntervalMs;

  @Value("${ewma.monitoring.validation.self-healing-enabled:true}")
  private boolean selfHealingEnabled;

  @Value("${ewma.monitoring.validation.log-interval-count:5}")
  private int logIntervalCount;

  private final EwmaLoadBalancer loadBalancer;
  private final LoadBalancerConfig lbConfig;
  private final ABTestingConfig abTestingConfig;
  private final ThreadPoolMonitorService threadPoolMonitor;
  private final LoadBalancingStrategyService loadBalancingStrategyService;

  private final AtomicInteger validationCount = new AtomicInteger(0);
  private final Map<String, ValidationResult> validationHistory = new ConcurrentHashMap<>();

  public ConfigValidationService(EwmaLoadBalancer loadBalancer,
                                 LoadBalancerConfig lbConfig,
                                 ABTestingConfig abTestingConfig,
                                 ThreadPoolMonitorService threadPoolMonitor,
                                 LoadBalancingStrategyService loadBalancingStrategyService) {
    this.loadBalancer = loadBalancer;
    this.lbConfig = lbConfig;
    this.abTestingConfig = abTestingConfig;
    this.threadPoolMonitor = threadPoolMonitor;
    this.loadBalancingStrategyService = loadBalancingStrategyService;
  }

  @PostConstruct
  public void init() {
    log.info("Configuration validation service initialized");
    performInitialValidation();
  }

  @Scheduled(fixedRateString = "${ewma.monitoring.validation.interval-ms:60000}")
  public void scheduledValidation() {
    performComprehensiveValidation();
  }

  @Scheduled(fixedRate = 30000)
  public void checkStrategyRestoration() {
    if (loadBalancingStrategyService != null) {
      loadBalancingStrategyService.checkAndRestoreStrategy();
      loadBalancingStrategyService.adjustStrategyBasedOnMetrics();
    }
  }

  private void performInitialValidation() {
    log.info("Performing initial configuration validation...");
    List<ValidationResult> results = validateAll();
    logValidationResults(results, "Initial validation");
  }

  public List<ValidationResult> validateAll() {
    validationCount.incrementAndGet();
    List<ValidationResult> results = new ArrayList<>();

    results.add(validateLoadBalancerConfig());
    results.add(validateABTestingConfig());
    results.add(validateInstancesHealth());
    results.add(validateThreadPools());
    results.add(validateCircuitBreakers());

    results.forEach(result ->
      validationHistory.put(result.getCheckName() + "-" + System.currentTimeMillis(), result));

    if (selfHealingEnabled) {
      applySelfHealing(results);
    }

    return results;
  }

  private ValidationResult validateLoadBalancerConfig() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    double alpha = lbConfig.getAlpha();
    if (alpha <= 0 || alpha >= 1) {
      errors.add("Alpha value must be between 0 and 1, got: " + alpha);
    }

    long healthCheckInterval = lbConfig.getHealthCheck().getIntervalMs();
    if (healthCheckInterval < 5000) {
      warnings.add("Health check interval is very short: " + healthCheckInterval + "ms");
    }

    if (lbConfig.getRetry().getMaxAttempts() > 10) {
      warnings.add("Max retry attempts is high: " + lbConfig.getRetry().getMaxAttempts());
    }

    return new ValidationResult("load_balancer_config", errors, warnings);
  }

  private ValidationResult validateABTestingConfig() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (abTestingConfig.isEnabled()) {
      for (Map.Entry<String, ABTestingConfig.Experiment> entry :
        abTestingConfig.getExperiments().entrySet()) {

        String expId = entry.getKey();
        ABTestingConfig.Experiment exp = entry.getValue();

        double totalWeight = exp.getVariants().values().stream()
          .mapToDouble(Double::doubleValue)
          .sum();

        if (Math.abs(totalWeight - 1.0) > 0.01) {
          warnings.add("Experiment '" + expId + "' weights sum to " +
            totalWeight + " (should be 1.0)");
        }

        if (!exp.getInstanceMapping().isEmpty()) {
          for (String variant : exp.getVariants().keySet()) {
            if (!exp.getInstanceMapping().containsKey(variant)) {
              warnings.add("Experiment '" + expId + "' variant '" +
                variant + "' has no instance mapping");
            }
          }
        }
      }
    }

    return new ValidationResult("ab_testing_config", errors, warnings);
  }

  private ValidationResult validateInstancesHealth() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    int totalInstances = loadBalancer.getInstanceCount();
    int healthyInstances = loadBalancer.getHealthyInstanceCount();

    if (totalInstances == 0) {
      errors.add("No backend instances registered");
    } else if (healthyInstances == 0) {
      errors.add("No healthy backend instances available");
    } else if (healthyInstances < totalInstances * 0.3) {
      warnings.add("Less than 30% of instances are healthy: " +
        healthyInstances + "/" + totalInstances);
    }

    loadBalancer.getAllInstances().forEach(instance -> {
      if (instance.getEwmaLatencyMs() > 1000) {
        warnings.add("Instance " + instance.getId() + " has high latency: " +
          instance.getEwmaLatencyMs() + "ms");
      }
    });

    return new ValidationResult("instances_health", errors, warnings);
  }

  private ValidationResult validateThreadPools() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Map<String, Object> lbStats = threadPoolMonitor.getLoadBalancerPoolStats();
    int activeCount = (Integer) lbStats.get("activeCount");
    int maxPoolSize = (Integer) lbStats.get("maximumPoolSize");
    int queueSize = (Integer) lbStats.get("queueSize");

    if (activeCount >= maxPoolSize * 0.9) {
      warnings.add("Load balancer thread pool is at " +
        Math.round((double) activeCount / maxPoolSize * 100) +
        "% capacity");
    }

    if (queueSize > 100) {
      warnings.add("Load balancer thread pool queue has " + queueSize + " pending tasks");
    }

    return new ValidationResult("thread_pools", errors, warnings);
  }

  private ValidationResult validateCircuitBreakers() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Map<String, Object> cbStats = loadBalancer.getCircuitBreakerStats();
    int openCount = 0;
    int halfOpenCount = 0;

    for (Map.Entry<String, Object> entry : cbStats.entrySet()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> instanceStats = (Map<String, Object>) entry.getValue();
      String state = (String) instanceStats.get("state");

      if ("OPEN".equals(state)) {
        openCount++;
      } else if ("HALF_OPEN".equals(state)) {
        halfOpenCount++;
      }
    }

    if (openCount > 0) {
      warnings.add(openCount + " circuit breakers are in OPEN state");
    }

    if (halfOpenCount > 0) {
      warnings.add(halfOpenCount + " circuit breakers are in HALF_OPEN state");
    }

    return new ValidationResult("circuit_breakers", errors, warnings);
  }

  private void applySelfHealing(List<ValidationResult> results) {
    for (ValidationResult result : results) {
      switch (result.getCheckName()) {
        case "thread_pools":
          healThreadPools(result);
          break;
        case "instances_health":
          healInstancesHealth(result);
          break;
        case "circuit_breakers":
          healCircuitBreakers(result);
          break;
      }
    }
  }

  private void healThreadPools(ValidationResult result) {
    if (!result.getWarnings().isEmpty()) {
      Map<String, Object> stats = threadPoolMonitor.getLoadBalancerPoolStats();
      int queueSize = (Integer) stats.get("queueSize");
      int activeCount = (Integer) stats.get("activeCount");
      int maxPoolSize = (Integer) stats.get("maximumPoolSize");

      if (queueSize > 50 && activeCount >= maxPoolSize * 0.8) {
        log.info("Self-healing: Scaling up thread pool due to high load");
        threadPoolMonitor.dynamicScaleUp();
      } else if (queueSize == 0 && activeCount < maxPoolSize * 0.3) {
        log.info("Self-healing: Scaling down thread pool due to low load");
        threadPoolMonitor.dynamicScaleDown();
      }
    }
  }

  private void healInstancesHealth(ValidationResult result) {
    if (result.getErrors().stream().anyMatch(e -> e.contains("No healthy"))) {
      log.warn("Self-healing: No healthy instances detected, forcing emergency health checks");
      triggerEmergencyHealthChecks();
    }

    if (result.getWarnings().stream().anyMatch(w -> w.contains("high latency"))) {
      log.info("Self-healing: High latency detected, adjusting load balancing strategy");
      if (loadBalancingStrategyService != null) {
        loadBalancingStrategyService.adjustLoadBalancingForHighLatency();
      }
    }
  }

  private void healCircuitBreakers(ValidationResult result) {
    if (!result.getWarnings().isEmpty()) {
      int halfOpenCount = 0;

      for (EwmaInstance instance : loadBalancer.getAllInstances()) {
        CircuitBreaker cb = instance.getCircuitBreaker();
        CircuitBreaker.State state = cb.getState();

        if (state == CircuitBreaker.State.HALF_OPEN) {
          halfOpenCount++;
          if (cb.getFailureCount() == 0) {
            forceCloseCircuitBreaker(instance.getId());
          }
        } else if (state == CircuitBreaker.State.OPEN) {
          attemptHalfOpenTransition(instance);
        }
      }

      log.info("Self-healing: Processed {} half-open circuit breakers", halfOpenCount);
    }
  }

  private void triggerEmergencyHealthChecks() {
    log.info("Triggering emergency health checks for all instances");

    EmergencyHealthCheckService emergencyService = new EmergencyHealthCheckService(loadBalancer);
    emergencyService.performEmergencyChecks();

    int healthyCount = loadBalancer.getHealthyInstanceCount();
    if (healthyCount > 0) {
      log.info("Emergency health checks recovered {} healthy instances", healthyCount);
    } else {
      log.error("Emergency health checks failed to recover any healthy instances");
    }
  }

  private void forceCloseCircuitBreaker(String instanceId) {
    log.info("Forcing circuit breaker CLOSED for instance: {}", instanceId);

    loadBalancer.getAllInstances().stream()
      .filter(instance -> instance.getId().equals(instanceId))
      .findFirst()
      .ifPresent(instance -> {
        try {
          var circuitBreaker = instance.getCircuitBreaker();
          circuitBreaker.setState(CircuitBreaker.State.CLOSED);

          AtomicInteger failures = circuitBreaker.getFailures();
          failures.set(0);

          log.debug("Successfully forced circuit breaker CLOSED for {}", instanceId);
        } catch (Exception e) {
          log.warn("Failed to force close circuit breaker for {}: {}", instanceId, e.getMessage());
        }
      });
  }

  private void attemptHalfOpenTransition(EwmaInstance instance) {
    CircuitBreaker cb = instance.getCircuitBreaker();

    AtomicLong lastFailureTime = cb.getLastFailureTime();

    if (lastFailureTime != null && System.currentTimeMillis() - lastFailureTime.get() > 30000) {
      log.info("Attempting to transition circuit breaker to HALF_OPEN for instance: {}", instance.getId());
      instance.recordSuccess();
    }
  }

  private static class EmergencyHealthCheckService {

    @Value("${ewma.loadbalancer.health-check.emergency-timeout-ms:2000}")
    private int emergencyHealthCheckTimeoutMs;

    private final EwmaLoadBalancer loadBalancer;

    public EmergencyHealthCheckService(EwmaLoadBalancer loadBalancer) {
      this.loadBalancer = loadBalancer;
    }

    public void performEmergencyChecks() {
        loadBalancer.getAllInstances().forEach(instance -> {
          try {
            boolean healthy = performQuickHealthCheck(instance);
            instance.setHealthy(healthy);

            if (healthy) {
              instance.recordSuccess();
              log.info("Emergency health check passed for instance: {}", instance.getId());
            } else {
              log.warn("Emergency health check failed for instance: {}", instance.getId());
            }
          } catch (Exception e) {
            log.error("Emergency health check error for instance {}: {}", instance.getId(), e.getMessage());
          }
        });
      }

      private boolean performQuickHealthCheck(EwmaInstance instance) {
        try {
          HttpURLConnection connection =
            (HttpURLConnection) new URL(instance.getUrl() + "/health").openConnection();

          connection.setConnectTimeout(emergencyHealthCheckTimeoutMs);
          connection.setReadTimeout(emergencyHealthCheckTimeoutMs);
          connection.setRequestMethod("GET");

          int responseCode = connection.getResponseCode();
          return responseCode >= 200 && responseCode < 300;

        } catch (Exception e) {
          return false;
        }
      }
    }

  private void logValidationResults(List<ValidationResult> results, String context) {
    int totalErrors = results.stream().mapToInt(r -> r.getErrors().size()).sum();
    int totalWarnings = results.stream().mapToInt(r -> r.getWarnings().size()).sum();

    if (totalErrors > 0 || totalWarnings > 0) {
      log.warn("{} - Errors: {}, Warnings: {}", context, totalErrors, totalWarnings);
      results.forEach(result -> {
        if (!result.getErrors().isEmpty() || !result.getWarnings().isEmpty()) {
          log.debug("Check '{}': Errors: {}, Warnings: {}",
            result.getCheckName(), result.getErrors().size(), result.getWarnings().size());
        }
      });
    } else {
      log.debug("{} - All checks passed", context);
    }
  }

  private void performComprehensiveValidation() {
    List<ValidationResult> results = validateAll();
    if (validationCount.get() % logIntervalCount == 0) {
      logValidationResults(results, "Scheduled validation #" + validationCount.get());
    }
  }

  public Map<String, Object> getValidationStats() {
    return Map.of(
      "totalValidations", validationCount.get(),
      "selfHealingEnabled", selfHealingEnabled,
      "validationHistorySize", validationHistory.size(),
      "recentResults", validationHistory.values().stream()
        .filter(r -> !r.getErrors().isEmpty() || !r.getWarnings().isEmpty())
        .limit(10)
        .toList()
    );
  }

  public void setSelfHealingEnabled(boolean enabled) {
    this.selfHealingEnabled = enabled;
    log.info("Self-healing {}", enabled ? "enabled" : "disabled");
  }

  public static class ValidationResult {
    private final String checkName;
    private final List<String> errors;
    private final List<String> warnings;
    private final long timestamp;

    public ValidationResult(String checkName, List<String> errors, List<String> warnings) {
      this.checkName = checkName;
      this.errors = new ArrayList<>(errors);
      this.warnings = new ArrayList<>(warnings);
      this.timestamp = System.currentTimeMillis();
    }

    public String getCheckName() { return checkName; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public long getTimestamp() { return timestamp; }
    public boolean isHealthy() { return errors.isEmpty(); }
  }
}
