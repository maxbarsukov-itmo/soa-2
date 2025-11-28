package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetricsService {

  private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

  private final EwmaLoadBalancer loadBalancer;

  private final AtomicLong totalRequestsCounter = new AtomicLong(0);
  private final AtomicLong successfulRequestsCounter = new AtomicLong(0);
  private final AtomicLong failedRequestsCounter = new AtomicLong(0);
  private final AtomicLong totalLatencyCounter = new AtomicLong(0);
  private final ConcurrentMap<String, AtomicLong> requestsPerInstance = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, AtomicLong> errorsPerInstance = new ConcurrentHashMap<>();

  public MetricsService(EwmaLoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
  }

  @PostConstruct
  public void init() {
    log.info("Metrics service initialized");
  }

  public void recordRequest(String instanceId, long latencyMs, boolean success) {
    totalRequestsCounter.incrementAndGet();
    totalLatencyCounter.addAndGet(latencyMs);

    if (success) {
      successfulRequestsCounter.incrementAndGet();
    } else {
      failedRequestsCounter.incrementAndGet();
    }

    requestsPerInstance.computeIfAbsent(instanceId, k -> new AtomicLong(0))
      .incrementAndGet();

    if (!success) {
      errorsPerInstance.computeIfAbsent(instanceId, k -> new AtomicLong(0))
        .incrementAndGet();
    }
  }

  @Scheduled(fixedRate = 30000)
  public void logMetrics() {
    long total = totalRequestsCounter.get();
    if (total == 0) return;

    double successRate = (double) successfulRequestsCounter.get() / total * 100;
    double avgLatency = (double) totalLatencyCounter.get() / total;

    log.info("""
              Load Balancer Metrics:
              Total Requests: {}
              Success Rate: {}%
              Average Latency: {}ms
              Healthy Instances: {}/{}
            """,
      total,
      String.format("%.2fms", successRate),
      String.format("%.2fms", avgLatency),
      loadBalancer.getHealthyInstanceCount(),
      loadBalancer.getInstanceCount()
    );
  }

  public Map<String, Object> getMetrics() {
    long total = totalRequestsCounter.get();
    double avgLatency = total > 0 ? (double) totalLatencyCounter.get() / total : 0;
    double successRateValue = total > 0 ? (double) successfulRequestsCounter.get() / total * 100 : 0;

    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "totalRequests", total,
      "successfulRequests", successfulRequestsCounter.get(),
      "failedRequests", failedRequestsCounter.get(),
      "averageLatencyMs", Math.round(avgLatency * 100.0) / 100.0,
      "successRate", Math.round(successRateValue * 100.0) / 100.0,
      "healthyInstances", loadBalancer.getHealthyInstanceCount(),
      "totalInstances", loadBalancer.getInstanceCount(),
      "instances", requestsPerInstance.entrySet().stream()
        .map(entry -> {
          String instanceId = entry.getKey();
          long requests = entry.getValue().get();
          long errors = errorsPerInstance.getOrDefault(instanceId, new AtomicLong(0)).get();
          double errorRate = requests > 0 ?
            Math.round((double) errors / requests * 10000.0) / 100.0 : 0;

          return Map.of(
            "instanceId", instanceId,
            "requests", requests,
            "errors", errors,
            "errorRate", errorRate
          );
        })
        .collect(Collectors.toList())
    );
  }

  public long getTotalRequests() {
    return totalRequestsCounter.get();
  }

  public long getSuccessfulRequests() {
    return successfulRequestsCounter.get();
  }

  public long getFailedRequests() {
    return failedRequestsCounter.get();
  }

  public double getSuccessRate() {
    long total = totalRequestsCounter.get();
    return total > 0 ?
      (double) successfulRequestsCounter.get() / total * 100 : 0;
  }

  public double getAverageLatency() {
    long total = totalRequestsCounter.get();
    return total > 0 ?
      (double) totalLatencyCounter.get() / total : 0;
  }
}
