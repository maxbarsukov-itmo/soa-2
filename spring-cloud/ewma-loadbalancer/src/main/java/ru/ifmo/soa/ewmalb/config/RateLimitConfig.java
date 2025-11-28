package ru.ifmo.soa.ewmalb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ewma.security.rate-limit")
public class RateLimitConfig {

  private boolean enabled = true;
  private Map<String, RateLimitRule> rules = new HashMap<>();
  private GlobalRule global = new GlobalRule();

  public static class RateLimitRule {
    private int requestsPerMinute = 100;
    private int burstCapacity = 150;
    private boolean byIp = true;
    private boolean byUser = false;
    private String pathPattern = "/**";

    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }

    public int getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

    public boolean isByIp() { return byIp; }
    public void setByIp(boolean byIp) { this.byIp = byIp; }

    public boolean isByUser() { return byUser; }
    public void setByUser(boolean byUser) { this.byUser = byUser; }

    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
  }

  public static class GlobalRule {
    private int requestsPerMinute = 1000;
    private int burstCapacity = 1500;
    private boolean enabled = true;

    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }

    public int getBurstCapacity() { return burstCapacity; }
    public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
  }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Map<String, RateLimitRule> getRules() { return rules; }
  public void setRules(Map<String, RateLimitRule> rules) { this.rules = rules; }

  public GlobalRule getGlobal() { return global; }
  public void setGlobal(GlobalRule global) { this.global = global; }
}
