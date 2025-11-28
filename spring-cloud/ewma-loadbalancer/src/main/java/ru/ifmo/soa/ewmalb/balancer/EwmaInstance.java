package ru.ifmo.soa.ewmalb.balancer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class EwmaInstance {

  private static final double ALPHA = 0.2;

  private final String serviceName;
  private final String id;
  private final String address;
  private final int port;

  private double ewmaLatencyMs = 100.0;
  private long lastResponse = System.currentTimeMillis();
  private long lastHealthCheck = System.currentTimeMillis();
  private volatile boolean healthy = true;
  private final AtomicInteger activeRequests = new AtomicInteger(0);
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong totalLatency = new AtomicLong(0);

  private final CircuitBreaker circuitBreaker;
  private final ReentrantLock lock = new ReentrantLock();

  public EwmaInstance(String serviceName, String id, String address, int port,
                      int failureThreshold, long resetTimeoutMs, long halfOpenTimeoutMs) {
    this.serviceName = serviceName;
    this.id = id;
    this.address = address;
    this.port = port;
    this.circuitBreaker = new CircuitBreaker(failureThreshold, resetTimeoutMs, halfOpenTimeoutMs);
  }

  public void updateLatency(long observedMs) {
    lock.lock();
    try {
      ewmaLatencyMs = ALPHA * observedMs + (1 - ALPHA) * ewmaLatencyMs;
      lastResponse = System.currentTimeMillis();
      totalRequests.incrementAndGet();
      totalLatency.addAndGet(observedMs);
    } finally {
      lock.unlock();
    }
  }

  public void updateLatencyWithAlpha(long observedMs, double alpha) {
    lock.lock();
    try {
      ewmaLatencyMs = alpha * observedMs + (1 - alpha) * ewmaLatencyMs;
      lastResponse = System.currentTimeMillis();
      totalRequests.incrementAndGet();
      totalLatency.addAndGet(observedMs);
    } finally {
      lock.unlock();
    }
  }

  public void recordSuccess() {
    circuitBreaker.recordSuccess();
    healthy = true;
  }

  public void recordFailure() {
    circuitBreaker.recordFailure();
  }

  public boolean allowRequest() {
    return circuitBreaker.allowRequest() && healthy;
  }

  public void requestStarted() {
    activeRequests.incrementAndGet();
  }

  public void requestCompleted() {
    activeRequests.decrementAndGet();
  }

  public String getServiceName() {
    return serviceName;
  }

  public double getPredictedLatency() {
    long timeSinceLastUpdate = System.currentTimeMillis() - lastResponse;
    if (timeSinceLastUpdate > 60000) {
      return ewmaLatencyMs * 1.5;
    }
    return ewmaLatencyMs;
  }

  public double getAverageLatency() {
    long requests = totalRequests.get();
    if (requests == 0) return 0;
    return (double) totalLatency.get() / requests;
  }

  public String getUrl() {
    return "http://" + address + ":" + port;
  }

  public boolean isHealthy() {
    return healthy && circuitBreaker.getState() == CircuitBreaker.State.CLOSED;
  }

  public String getId() { return id; }
  public String getAddress() { return address; }
  public int getPort() { return port; }
  public double getEwmaLatencyMs() { return ewmaLatencyMs; }
  public long getLastResponse() { return lastResponse; }
  public void setHealthy(boolean healthy) { this.healthy = healthy; }
  public int getActiveRequests() { return activeRequests.get(); }
  public long getTotalRequests() { return totalRequests.get(); }
  public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
  public void setLastHealthCheck(long lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }

  @Override
  public String toString() {
      return String.format("EwmaInstance{id='%s', %s:%d, ewma=%.2fms, healthy=%s, active=%d, circuit=%s}",
        id, address, port, ewmaLatencyMs, healthy, activeRequests.get(), circuitBreaker.getState());
  }
}
