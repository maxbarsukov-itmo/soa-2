package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.config.RateLimitConfig;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

  private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

  private final RateLimitConfig config;
  private final Map<String, RequestBucket> ipBuckets = new ConcurrentHashMap<>();
  private final Map<String, RequestBucket> userBuckets = new ConcurrentHashMap<>();
  private final RequestBucket globalBucket;
  private final ScheduledExecutorService cleanupExecutor;

  public RateLimitService(RateLimitConfig config) {
    this.config = config;
    this.globalBucket = new RequestBucket(
      config.getGlobal().getRequestsPerMinute(),
      config.getGlobal().getBurstCapacity()
    );
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "rate-limit-cleanup");
      t.setDaemon(true);
      return t;
    });
  }

  @PostConstruct
  public void init() {
    if (config.isEnabled()) {
      cleanupExecutor.scheduleAtFixedRate(this::cleanupOldBuckets, 5, 5, TimeUnit.MINUTES);
      log.info("Rate limiting service initialized with {} rules", config.getRules().size());
    } else {
      log.info("Rate limiting service is disabled");
    }
  }

  public boolean allowRequest(HttpServletRequest request, String path) {
    if (!config.isEnabled()) {
      return true;
    }

    String clientId = getClientIdentifier(request);
    boolean allowed = true;

    if (config.getGlobal().isEnabled()) {
      allowed = globalBucket.allowRequest();
      if (!allowed) {
        log.warn("Global rate limit exceeded for client: {}", clientId);
        return false;
      }
    }

    for (Map.Entry<String, RateLimitConfig.RateLimitRule> entry : config.getRules().entrySet()) {
      RateLimitConfig.RateLimitRule rule = entry.getValue();

      if (pathMatchesPattern(path, rule.getPathPattern())) {
        if (rule.isByIp()) {
          String ip = getClientIp(request);
          RequestBucket bucket = ipBuckets.computeIfAbsent(ip,
            k -> new RequestBucket(rule.getRequestsPerMinute(), rule.getBurstCapacity()));

          if (!bucket.allowRequest()) {
            log.warn("IP rate limit exceeded for {} on path {}", ip, path);
            return false;
          }
        }

        if (rule.isByUser()) {
          String userId = extractSessionId(request);
          if (userId != null) {
            RequestBucket bucket = userBuckets.computeIfAbsent(userId,
              k -> new RequestBucket(rule.getRequestsPerMinute(), rule.getBurstCapacity()));

            if (!bucket.allowRequest()) {
              log.warn("User rate limit exceeded for {} on path {}", userId, path);
              return false;
            }
          }
        }
      }
    }

    return true;
  }

  public Map<String, Object> getRateLimitStats(String clientId) {
    Map<String, Object> stats = new java.util.HashMap<>();

    if (config.getGlobal().isEnabled()) {
      stats.put("global", globalBucket.getStats());
    }

    if (clientId != null) {
      RequestBucket ipBucket = ipBuckets.get(getClientIpFromId(clientId));
      RequestBucket userBucket = userBuckets.get(clientId);

      if (ipBucket != null) {
        stats.put("ip", ipBucket.getStats());
      }
      if (userBucket != null) {
        stats.put("user", userBucket.getStats());
      }
    }

    return stats;
  }

  public void resetBucket(String bucketType, String identifier) {
    switch (bucketType.toLowerCase()) {
      case "ip":
        ipBuckets.remove(identifier);
        log.info("Reset IP rate limit bucket for: {}", identifier);
        break;
      case "user":
        userBuckets.remove(identifier);
        log.info("Reset user rate limit bucket for: {}", identifier);
        break;
      case "global":
        globalBucket.reset();
        log.info("Reset global rate limit bucket");
        break;
    }
  }

  private String getClientIdentifier(HttpServletRequest request) {
    String ip = getClientIp(request);
    String userId = extractSessionId(request);
    return userId != null ? userId + "@" + ip : ip;
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  private String getClientIpFromId(String clientId) {
    if (clientId.contains("@")) {
      return clientId.split("@")[1];
    }
    return clientId;
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

  private boolean pathMatchesPattern(String path, String pattern) {
    if ("/**".equals(pattern)) {
      return true;
    }
    if (pattern.endsWith("/**")) {
      String prefix = pattern.substring(0, pattern.length() - 3);
      return path.startsWith(prefix);
    }
    return path.equals(pattern);
  }

  private void cleanupOldBuckets() {
    Instant now = Instant.now();
    Instant cutoff = now.minusSeconds(300);

    ipBuckets.entrySet().removeIf(entry ->
      entry.getValue().getLastAccess().isBefore(cutoff));

    userBuckets.entrySet().removeIf(entry ->
      entry.getValue().getLastAccess().isBefore(cutoff));

    log.debug("Rate limit buckets cleanup completed. IP buckets: {}, User buckets: {}",
      ipBuckets.size(), userBuckets.size());
  }

  private static class RequestBucket {
    private final int requestsPerMinute;
    private final int burstCapacity;
    private final java.util.Queue<Instant> requests;
    private Instant lastAccess;

    public RequestBucket(int requestsPerMinute, int burstCapacity) {
      this.requestsPerMinute = requestsPerMinute;
      this.burstCapacity = burstCapacity;
      this.requests = new java.util.LinkedList<>();
      this.lastAccess = Instant.now();
    }

    public synchronized boolean allowRequest() {
      lastAccess = Instant.now();
      Instant now = Instant.now();
      Instant windowStart = now.minusSeconds(60);

      while (!requests.isEmpty() && requests.peek().isBefore(windowStart)) {
        requests.poll();
      }

      if (requests.size() >= burstCapacity) {
        return false;
      }

      if (requests.size() < requestsPerMinute) {
        requests.offer(now);
        return true;
      }

      return false;
    }

    public synchronized Map<String, Object> getStats() {
      Instant now = Instant.now();
      Instant windowStart = now.minusSeconds(60);

      while (!requests.isEmpty() && requests.peek().isBefore(windowStart)) {
        requests.poll();
      }

      return Map.of(
        "requestsInWindow", requests.size(),
        "limitPerMinute", requestsPerMinute,
        "burstCapacity", burstCapacity,
        "lastAccess", lastAccess.toString()
      );
    }

    public synchronized void reset() {
      requests.clear();
      lastAccess = Instant.now();
    }

    public Instant getLastAccess() {
      return lastAccess;
    }
  }
}
