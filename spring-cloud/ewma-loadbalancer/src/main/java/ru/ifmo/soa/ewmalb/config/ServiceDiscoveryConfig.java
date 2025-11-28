package ru.ifmo.soa.ewmalb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "service")
public class ServiceDiscoveryConfig {
  private String host = "localhost";
  private List<String> backends = new ArrayList<>();
  private Registration registration = new Registration();

  public static class Registration {
    private boolean enabled = true;
    private String healthCheckUrl;
    private java.util.List<String> tags = new java.util.ArrayList<>();
    private java.util.Map<String, String> meta = new java.util.HashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHealthCheckUrl() { return healthCheckUrl; }
    public void setHealthCheckUrl(String healthCheckUrl) { this.healthCheckUrl = healthCheckUrl; }
    public java.util.List<String> getTags() { return tags; }
    public void setTags(java.util.List<String> tags) { this.tags = tags; }
    public java.util.Map<String, String> getMeta() { return meta; }
    public void setMeta(java.util.Map<String, String> meta) { this.meta = meta; }
  }

  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public List<String> getBackends() { return backends; }
  public void setBackends(List<String> backends) { this.backends = backends; }
  public Registration getRegistration() { return registration; }
  public void setRegistration(Registration registration) { this.registration = registration; }
}
