package ru.ifmo.soa.ewmalb.balancer;

import java.util.concurrent.locks.ReentrantLock;

public class EwmaInstance {

  private static final double ALPHA = 0.2;

  private final String id;
  private final String address;
  private final int port;
  private double ewmaLatencyMs = 100.0;
  private long lastResponse = System.currentTimeMillis();

  private final ReentrantLock lock = new ReentrantLock();

  public EwmaInstance(String id, String address, int port) {
    this.id = id;
    this.address = address;
    this.port = port;
  }

  public void updateLatency(long observedMs) {
    lock.lock();
    try {
      ewmaLatencyMs = ALPHA * observedMs + (1 - ALPHA) * ewmaLatencyMs;
      lastResponse = System.currentTimeMillis();
    } finally {
      lock.unlock();
    }
  }

  public String getUrl() {
    return "http://" + address + ":" + port;
  }

  public String getId() { return id; }
  public String getAddress() { return address; }
  public int getPort() { return port; }
  public double getEwmaLatencyMs() { return ewmaLatencyMs; }
  public long getLastResponse() { return lastResponse; }

  @Override
  public String toString() {
    return String.format("EwmaInstance{id='%s', %s:%d, ewma=%.2fms}", id, address, port, ewmaLatencyMs);
  }
}
