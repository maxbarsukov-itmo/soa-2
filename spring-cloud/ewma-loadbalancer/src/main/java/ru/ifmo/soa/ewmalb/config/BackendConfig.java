package ru.ifmo.soa.ewmalb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ewma")
public class BackendConfig {
  private String host = "localhost";
  private Map<String, BackendSpec> backends = new HashMap<>();

  public static class BackendSpec {
    private HealthCheck healthCheck = new HealthCheck();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private Retry retry = new Retry();
    private Selection selection = new Selection();

    public HealthCheck getHealthCheck() { return healthCheck; }
    public void setHealthCheck(HealthCheck healthCheck) { this.healthCheck = healthCheck; }
    public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) { this.circuitBreaker = circuitBreaker; }
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
    public Selection getSelection() { return selection; }
    public void setSelection(Selection selection) { this.selection = selection; }

    public static class HealthCheck {
      private String path = "/health";
      private long intervalMs = 30000;
      private int timeoutMs = 5000;
      public String getPath() { return path; }
      public void setPath(String path) { this.path = path; }
      public long getIntervalMs() { return intervalMs; }
      public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
      public int getTimeoutMs() { return timeoutMs; }
      public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class CircuitBreaker {
      private int failureThreshold = 5;
      private long resetTimeoutMs = 60000;
      private long halfOpenTimeoutMs = 30000;
      public int getFailureThreshold() { return failureThreshold; }
      public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
      public long getResetTimeoutMs() { return resetTimeoutMs; }
      public void setResetTimeoutMs(long resetTimeoutMs) { this.resetTimeoutMs = resetTimeoutMs; }
      public long getHalfOpenTimeoutMs() { return halfOpenTimeoutMs; }
      public void setHalfOpenTimeoutMs(long halfOpenTimeoutMs) { this.halfOpenTimeoutMs = halfOpenTimeoutMs; }
    }

    public static class Retry {
      private boolean enabled = true;
      private int maxAttempts = 3;
      private long backoffMs = 100;
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public int getMaxAttempts() { return maxAttempts; }
      public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
      public long getBackoffMs() { return backoffMs; }
      public void setBackoffMs(long backoffMs) { this.backoffMs = backoffMs; }
    }

    public static class Selection {
      private String strategy = "WEIGHTED_RANDOM";
      public String getStrategy() { return strategy; }
      public void setStrategy(String strategy) { this.strategy = strategy; }
    }
  }

  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public Map<String, BackendSpec> getBackends() { return backends; }
  public void setBackends(Map<String, BackendSpec> backends) { this.backends = backends; }
}
