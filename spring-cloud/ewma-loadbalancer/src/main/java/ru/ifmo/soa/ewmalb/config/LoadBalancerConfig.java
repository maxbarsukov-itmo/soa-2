package ru.ifmo.soa.ewmalb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ewma.loadbalancer")
public class LoadBalancerConfig {

  private double alpha = 0.2;
  private AdaptiveAlpha adaptiveAlpha = new AdaptiveAlpha();
  private Selection selection = new Selection();
  private CircuitBreaker circuitBreaker = new CircuitBreaker();
  private HealthCheck healthCheck = new HealthCheck();
  private Retry retry = new Retry();
  private StickySessions stickySessions = new StickySessions();
  private GracefulDegradation gracefulDegradation = new GracefulDegradation();
  private PredictiveLatency predictiveLatency = new PredictiveLatency();

  public static class AdaptiveAlpha {

    private boolean enabled = true;
    private double baseAlpha = 0.2;
    private double maxAlpha = 0.4;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public double getBaseAlpha() {
      return baseAlpha;
    }

    public void setBaseAlpha(double baseAlpha) {
      this.baseAlpha = baseAlpha;
    }

    public double getMaxAlpha() {
      return maxAlpha;
    }

    public void setMaxAlpha(double maxAlpha) {
      this.maxAlpha = maxAlpha;
    }
  }

  public static class Selection {

    private String strategy = "WEIGHTED_RANDOM";
    private WeightedRandom weightedRandom = new WeightedRandom();
    private LoadAware loadAware = new LoadAware();

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }

    public WeightedRandom getWeightedRandom() {
      return weightedRandom;
    }

    public void setWeightedRandom(WeightedRandom weightedRandom) {
      this.weightedRandom = weightedRandom;
    }

    public LoadAware getLoadAware() {
      return loadAware;
    }

    public void setLoadAware(LoadAware loadAware) {
      this.loadAware = loadAware;
    }
  }

  public static class WeightedRandom {

    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class LoadAware {

    private int maxConcurrentRequests = 100;

    public int getMaxConcurrentRequests() {
      return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
      this.maxConcurrentRequests = maxConcurrentRequests;
    }
  }

  public static class CircuitBreaker {

    private boolean enabled = true;
    private int failureThreshold = 5;
    private long resetTimeoutMs = 60000;
    private long halfOpenTimeoutMs = 30000;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getFailureThreshold() {
      return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
      this.failureThreshold = failureThreshold;
    }

    public long getResetTimeoutMs() {
      return resetTimeoutMs;
    }

    public void setResetTimeoutMs(long resetTimeoutMs) {
      this.resetTimeoutMs = resetTimeoutMs;
    }

    public long getHalfOpenTimeoutMs() {
      return halfOpenTimeoutMs;
    }

    public void setHalfOpenTimeoutMs(long halfOpenTimeoutMs) {
      this.halfOpenTimeoutMs = halfOpenTimeoutMs;
    }
  }

  public static class HealthCheck {

    private boolean enabled = true;
    private long intervalMs = 30000;
    private long timeoutMs = 5000;
    private String path = "/api/v1/health";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public long getIntervalMs() {
      return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
      this.intervalMs = intervalMs;
    }

    public long getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
      this.timeoutMs = timeoutMs;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }

  public static class Retry {

    private boolean enabled = true;
    private int maxAttempts = 3;
    private long backoffMs = 100;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getBackoffMs() {
      return backoffMs;
    }

    public void setBackoffMs(long backoffMs) {
      this.backoffMs = backoffMs;
    }
  }

  public static class StickySessions {

    private boolean enabled = false;
    private int timeoutMinutes = 30;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getTimeoutMinutes() {
      return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
      this.timeoutMinutes = timeoutMinutes;
    }
  }

  public static class GracefulDegradation {
    private boolean enabled = true;
    private boolean allowUnhealthy = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isAllowUnhealthy() {
      return allowUnhealthy;
    }

    public void setAllowUnhealthy(boolean allowUnhealthy) {
      this.allowUnhealthy = allowUnhealthy;
    }
  }

  public static class PredictiveLatency {
    private boolean enabled = true;
    private double coldInstancePenalty = 1.5;
    private long coldTimeoutMs = 60000;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public double getColdInstancePenalty() {
      return coldInstancePenalty;
    }

    public void setColdInstancePenalty(double coldInstancePenalty) {
      this.coldInstancePenalty = coldInstancePenalty;
    }

    public long getColdTimeoutMs() {
      return coldTimeoutMs;
    }

    public void setColdTimeoutMs(long coldTimeoutMs) {
      this.coldTimeoutMs = coldTimeoutMs;
    }
  }

  public double getAlpha() {
    return alpha;
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  public AdaptiveAlpha getAdaptiveAlpha() {
    return adaptiveAlpha;
  }

  public void setAdaptiveAlpha(AdaptiveAlpha adaptiveAlpha) {
    this.adaptiveAlpha = adaptiveAlpha;
  }

  public Selection getSelection() {
    return selection;
  }

  public void setSelection(Selection selection) {
    this.selection = selection;
  }

  public CircuitBreaker getCircuitBreaker() {
    return circuitBreaker;
  }

  public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
    this.circuitBreaker = circuitBreaker;
  }

  public HealthCheck getHealthCheck() {
    return healthCheck;
  }

  public void setHealthCheck(HealthCheck healthCheck) {
    this.healthCheck = healthCheck;
  }

  public Retry getRetry() {
    return retry;
  }

  public void setRetry(Retry retry) {
    this.retry = retry;
  }

  public StickySessions getStickySessions() {
    return stickySessions;
  }

  public void setStickySessions(StickySessions stickySessions) {
    this.stickySessions = stickySessions;
  }

  public GracefulDegradation getGracefulDegradation() {
    return gracefulDegradation;
  }

  public void setGracefulDegradation(GracefulDegradation gracefulDegradation) {
    this.gracefulDegradation = gracefulDegradation;
  }

  public PredictiveLatency getPredictiveLatency() {
    return predictiveLatency;
  }

  public void setPredictiveLatency(PredictiveLatency predictiveLatency) {
    this.predictiveLatency = predictiveLatency;
  }
}
