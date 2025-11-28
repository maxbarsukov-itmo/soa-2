package ru.ifmo.soa.ewmalb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ewma.loadbalancer.ab-testing")
public class ABTestingConfig {

  private boolean enabled = false;
  private String headerName = "X-Experiment";
  private String cookieName = "X-AB-Test";
  private Map<String, Experiment> experiments = new HashMap<>();

  public static class Experiment {
    private String name;
    private Map<String, Double> variants = new HashMap<>();
    private String defaultVariant = "control";
    private Map<String, String> instanceMapping = new HashMap<>();
    private boolean sticky = true;
    private int stickyTimeoutMinutes = 30;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Double> getVariants() { return variants; }
    public void setVariants(Map<String, Double> variants) { this.variants = variants; }

    public String getDefaultVariant() { return defaultVariant; }
    public void setDefaultVariant(String defaultVariant) { this.defaultVariant = defaultVariant; }

    public Map<String, String> getInstanceMapping() { return instanceMapping; }
    public void setInstanceMapping(Map<String, String> instanceMapping) { this.instanceMapping = instanceMapping; }

    public boolean isSticky() { return sticky; }
    public void setSticky(boolean sticky) { this.sticky = sticky; }

    public int getStickyTimeoutMinutes() { return stickyTimeoutMinutes; }
    public void setStickyTimeoutMinutes(int stickyTimeoutMinutes) { this.stickyTimeoutMinutes = stickyTimeoutMinutes; }
  }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public String getHeaderName() { return headerName; }
  public void setHeaderName(String headerName) { this.headerName = headerName; }

  public String getCookieName() { return cookieName; }
  public void setCookieName(String cookieName) { this.cookieName = cookieName; }

  public Map<String, Experiment> getExperiments() { return experiments; }
  public void setExperiments(Map<String, Experiment> experiments) { this.experiments = experiments; }
}
