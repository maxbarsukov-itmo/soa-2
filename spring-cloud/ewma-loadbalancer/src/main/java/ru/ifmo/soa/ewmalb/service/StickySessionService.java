package ru.ifmo.soa.ewmalb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StickySessionService {

  private static final Logger log = LoggerFactory.getLogger(StickySessionService.class);

  private final EwmaLoadBalancer loadBalancer;
  private final ConcurrentHashMap<String, SessionMapping> sessionMappings = new ConcurrentHashMap<>();

  @Value("${ewma.loadbalancer.sticky-sessions.enabled}")
  private boolean enabled;

  @Value("${ewma.loadbalancer.sticky-sessions.timeout-minutes}")
  private int sessionTimeoutMinutes;

  public StickySessionService(EwmaLoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
    ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions,
      1, 1, TimeUnit.MINUTES);
  }

  private String getKey(String sessionId, String service) {
    return service + ":" + sessionId;
  }

  public EwmaInstance getInstanceForService(String sessionId, String service) {
    if (!enabled) return loadBalancer.selectInstanceForService(service);
    String key = getKey(sessionId, service);
    SessionMapping mapping = sessionMappings.get(key);
    if (mapping != null && !mapping.isExpired()) {
      EwmaInstance instance = mapping.getInstance();
      if (instance != null && instance.isHealthy() && instance.getServiceName().equals(service)) {
        mapping.updateLastAccess();
        return instance;
      } else {
        sessionMappings.remove(key);
      }
    }
    EwmaInstance newInstance = loadBalancer.selectInstanceForService(service);
    if (newInstance != null) {
      sessionMappings.put(key, new SessionMapping(newInstance));
    }
    return newInstance;
  }

  public void updateSession(String sessionId, String service, EwmaInstance instance) {
    if (enabled && instance != null) {
      sessionMappings.put(getKey(sessionId, service), new SessionMapping(instance));
    }
  }

  public void removeSession(String sessionId) {
    sessionMappings.remove(sessionId);
  }

  private void cleanupExpiredSessions() {
    long now = System.currentTimeMillis();
    long expiredCount = sessionMappings.entrySet().stream()
      .filter(entry -> entry.getValue().isExpired(now))
      .peek(entry -> sessionMappings.remove(entry.getKey()))
      .count();

    if (expiredCount > 0) {
      log.debug("Cleaned up {} expired sticky sessions", expiredCount);
    }
  }

  public Map<String, Object> getSessionStats() {
    long now = System.currentTimeMillis();
    long activeSessions = sessionMappings.values().stream()
      .filter(mapping -> !mapping.isExpired(now))
      .count();

    return Map.of(
      "enabled", enabled,
      "timeoutMinutes", sessionTimeoutMinutes,
      "totalSessions", sessionMappings.size(),
      "activeSessions", activeSessions,
      "sessionMappings", sessionMappings.entrySet().stream()
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          entry -> Map.of(
            "instanceId", entry.getValue().getInstance().getId(),
            "lastAccess", entry.getValue().getLastAccess(),
            "expiresIn", entry.getValue().getExpiresIn(now)
          )
        ))
    );
  }

  private class SessionMapping {
    private final EwmaInstance instance;
    private volatile long lastAccess;

    SessionMapping(EwmaInstance instance) {
      this.instance = instance;
      this.lastAccess = System.currentTimeMillis();
    }

    EwmaInstance getInstance() { return instance; }
    long getLastAccess() { return lastAccess; }

    void updateLastAccess() {
      this.lastAccess = System.currentTimeMillis();
    }

    boolean isExpired() {
      return isExpired(System.currentTimeMillis());
    }

    boolean isExpired(long currentTime) {
      return currentTime - lastAccess > sessionTimeoutMinutes * 60 * 1000L;
    }

    long getExpiresIn(long currentTime) {
      return (lastAccess + sessionTimeoutMinutes * 60 * 1000L - currentTime) / 1000;
    }
  }
}
