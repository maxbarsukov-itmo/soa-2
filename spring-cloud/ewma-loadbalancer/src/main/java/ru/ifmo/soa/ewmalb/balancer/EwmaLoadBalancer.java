package ru.ifmo.soa.ewmalb.balancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class EwmaLoadBalancer {

  private static final Logger log = LoggerFactory.getLogger(EwmaLoadBalancer.class);

  private final ConcurrentHashMap<String, EwmaInstance> instances = new ConcurrentHashMap<>();

  public void registerInstance(String id, String address, int port) {
    instances.put(id, new EwmaInstance(id, address, port));
    log.info("Registered backend: {}", id);
  }

  public void unregisterInstance(String id) {
    if (instances.remove(id) != null) {
      log.info("Unregistered backend: {}", id);
    }
  }

  public void recordLatency(String id, long latencyMs) {
    EwmaInstance inst = instances.get(id);
    if (inst != null) {
      inst.updateLatency(latencyMs);
      log.debug("Updated RTT for {}: {}ms → EWMA={}ms",
        id, latencyMs, String.format("%.2f", inst.getEwmaLatencyMs()));
    }
  }

  public EwmaInstance selectInstance() {
    if (instances.isEmpty()) return null;

    return instances.values().stream()
      .min(Comparator.comparingDouble(EwmaInstance::getEwmaLatencyMs))
      .orElseGet(() -> {
        List<EwmaInstance> list = instances.values().stream().toList();
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
      });
  }

  public void logCurrentState() {
    log.info("=== EWMA Load Balancer State ===");
    if (instances.isEmpty()) {
      log.info("No backends registered");
    } else {
      instances.values().forEach(inst -> log.info(inst.toString()));
    }
  }}
