package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.config.ABTestingConfig;

import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ABTestingService {

  private static final Logger log = LoggerFactory.getLogger(ABTestingService.class);

  private final ABTestingConfig config;
  private final Map<String, NavigableMap<Double, String>> experimentWeights = new ConcurrentHashMap<>();
  private final Map<String, String> userAssignments = new ConcurrentHashMap<>();

  public ABTestingService(ABTestingConfig config) {
    this.config = config;
  }

  @PostConstruct
  public void init() {
    if (config.isEnabled()) {
      precomputeWeights();
      log.info("A/B Testing initialized with {} experiments", config.getExperiments().size());
    } else {
      log.info("A/B Testing is disabled");
    }
  }

  private void precomputeWeights() {
    config.getExperiments().forEach((expId, experiment) -> {
      NavigableMap<Double, String> weights = new TreeMap<>();
      double current = 0.0;

      for (Map.Entry<String, Double> variant : experiment.getVariants().entrySet()) {
        current += variant.getValue();
        weights.put(current, variant.getKey());
      }

      if (current != 1.0 && current > 0) {
        NavigableMap<Double, String> normalized = new TreeMap<>();
        double factor = 1.0 / current;
        double normalizedCurrent = 0.0;

        for (Map.Entry<Double, String> entry : weights.entrySet()) {
          normalizedCurrent += entry.getKey() * factor;
          normalized.put(normalizedCurrent, entry.getValue());
        }
        weights = normalized;
      }

      experimentWeights.put(expId, weights);
      log.debug("Experiment '{}' weights: {}", expId, weights);
    });
  }

  public String getExperimentIdForService(String serviceName) {
    if (!config.isEnabled()) {
      return null;
    }

    String exactMatch = "service_" + serviceName;
    if (config.getExperiments().containsKey(exactMatch)) {
      return exactMatch;
    }

    for (String expId : config.getExperiments().keySet()) {
      if (expId.startsWith("service_")) {
        if (expId.equals("service_*")) {
          return expId;
        }

        if (expId.endsWith("_*") && serviceName.startsWith(expId.substring(8, expId.length() - 2))) {
          return expId;
        }

        if (expId.startsWith("*_") && serviceName.endsWith(expId.substring(2))) {
          return expId;
        }
      }
    }

    return null;
  }

  public String getVariantForRequest(String experimentId, HttpServletRequest request) {
    if (!config.isEnabled() || !config.getExperiments().containsKey(experimentId)) {
      return null;
    }

    ABTestingConfig.Experiment experiment = config.getExperiments().get(experimentId);

    if (experiment.isSticky()) {
      String userId = getUserId(request);
      String assignmentKey = experimentId + ":" + userId;

      String assignedVariant = userAssignments.get(assignmentKey);
      if (assignedVariant != null) {
        log.debug("Using sticky assignment for user {}: {}", userId, assignedVariant);
        return assignedVariant;
      }
    }

    String forcedVariant = getForcedVariant(request, experimentId);
    if (forcedVariant != null && experiment.getVariants().containsKey(forcedVariant)) {
      log.debug("Using forced variant for experiment {}: {}", experimentId, forcedVariant);
      return forcedVariant;
    }

    String variant = assignVariant(experimentId, request);

    if (experiment.isSticky() && variant != null) {
      String userId = getUserId(request);
      String assignmentKey = experimentId + ":" + userId;
      userAssignments.put(assignmentKey, variant);
      log.debug("Assigned user {} to variant {} for experiment {}", userId, variant, experimentId);
    }

    return variant;
  }

  private String assignVariant(String experimentId, HttpServletRequest request) {
    NavigableMap<Double, String> weights = experimentWeights.get(experimentId);
    if (weights == null || weights.isEmpty()) {
      return config.getExperiments().get(experimentId).getDefaultVariant();
    }

    String userId = getUserId(request);
    double hash = hashToDouble(userId + experimentId);

    String variant = weights.ceilingEntry(hash).getValue();
    log.debug("Assigned variant {} for user {} (hash: {})", variant, userId, hash);

    return variant;
  }

  private String getForcedVariant(HttpServletRequest request, String experimentId) {
    String headerVariant = request.getHeader(config.getHeaderName() + "-" + experimentId);
    if (headerVariant != null && !headerVariant.trim().isEmpty()) {
      return headerVariant.trim();
    }

    String generalHeader = request.getHeader(config.getHeaderName());
    if (generalHeader != null && generalHeader.contains(":")) {
      String[] parts = generalHeader.split(":");
      if (parts.length == 2 && parts[0].equals(experimentId)) {
        return parts[1].trim();
      }
    }

    String cookieHeader = request.getHeader("Cookie");
    if (cookieHeader != null) {
      for (String cookie : cookieHeader.split(";")) {
        if (cookie.trim().startsWith(config.getCookieName() + "=")) {
          String cookieValue = cookie.split("=")[1].trim();
          if (cookieValue.contains(":")) {
            String[] parts = cookieValue.split(":");
            if (parts.length == 2 && parts[0].equals(experimentId)) {
              return parts[1].trim();
            }
          }
        }
      }
    }

    return null;
  }

  private String getUserId(HttpServletRequest request) {
    return extractSessionId(request);
  }

  protected String extractSessionId(HttpServletRequest request) {
    String sessionId = request.getHeader("X-Session-ID");
    if (sessionId != null) return sessionId;

    String cookieHeader = request.getHeader("Cookie");
    if (cookieHeader != null && cookieHeader.contains("JSESSIONID")) {
      for (String cookie : cookieHeader.split(";")) {
        if (cookie.trim().startsWith("JSESSIONID=")) {
          return cookie.split("=")[1].trim();
        }
      }
    }

    return null;
  }

  private double hashToDouble(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes());
      long hash = 0;
      for (int i = 0; i < 8; i++) {
        hash = (hash << 8) | (digest[i] & 0xFF);
      }
      return (double) (hash & 0x7FFFFFFFFFFFFFFFL) / Long.MAX_VALUE;
    } catch (NoSuchAlgorithmException e) {
      return Math.random();
    }
  }

  private String hashToHex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes());
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(input.hashCode());
    }
  }

  public EwmaInstance getInstanceForVariant(String experimentId, String variant, List<EwmaInstance> instances) {
    ABTestingConfig.Experiment experiment = config.getExperiments().get(experimentId);
    if (experiment == null) {
      return null;
    }

    String instanceGroup = experiment.getInstanceMapping().get(variant);
    if (instanceGroup != null) {
      return instances.stream()
        .filter(instance -> instance.getId().contains(instanceGroup) && instance.isHealthy())
        .findFirst()
        .orElse(null);
    }

    return null;
  }

  public Map<String, Object> getExperimentStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("enabled", config.isEnabled());
    stats.put("totalExperiments", config.getExperiments().size());
    stats.put("totalAssignments", userAssignments.size());

    Map<String, Object> experimentStats = new HashMap<>();
    config.getExperiments().forEach((expId, experiment) -> {
      Map<String, Long> variantCounts = new HashMap<>();
      userAssignments.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(expId + ":"))
        .forEach(entry -> {
          String variant = entry.getValue();
          variantCounts.put(variant, variantCounts.getOrDefault(variant, 0L) + 1);
        });

      experimentStats.put(expId, Map.of(
        "name", experiment.getName(),
        "variants", experiment.getVariants(),
        "assignments", variantCounts,
        "sticky", experiment.isSticky()
      ));
    });

    stats.put("experiments", experimentStats);
    return stats;
  }
}
