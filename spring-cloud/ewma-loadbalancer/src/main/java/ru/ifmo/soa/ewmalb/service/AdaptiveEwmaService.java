package ru.ifmo.soa.ewmalb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;

@Service
public class AdaptiveEwmaService {

  @Value("${ewma.loadbalancer.adaptive-alpha.enabled}")
  private boolean adaptiveAlphaEnabled;

  @Value("${ewma.loadbalancer.adaptive-alpha.base-alpha}")
  private double baseAlpha;

  @Value("${ewma.loadbalancer.adaptive-alpha.max-alpha}")
  private double maxAlpha;

  public double calculateAlpha(EwmaInstance instance, long observedLatency) {
    if (!adaptiveAlphaEnabled) {
      return baseAlpha;
    }

    double currentLatency = instance.getEwmaLatencyMs();
    if (currentLatency == 0) return baseAlpha;

    double deviation = Math.abs(observedLatency - currentLatency) / currentLatency;

    double adaptiveAlpha = baseAlpha;

    if (deviation > 0.5) {
      adaptiveAlpha = Math.min(maxAlpha, baseAlpha * 2);
    } else if (deviation > 0.2) {
      adaptiveAlpha = Math.min(maxAlpha, baseAlpha * 1.5);
    }

    double stabilityFactor = calculateStabilityFactor(instance);
    adaptiveAlpha *= stabilityFactor;

    return Math.max(0.1, Math.min(0.9, adaptiveAlpha));
  }

  private double calculateStabilityFactor(EwmaInstance instance) {
    long totalRequests = instance.getTotalRequests();
    if (totalRequests < 10) return 1.2;
    if (totalRequests > 1000) return 0.8;

    return 1.0;
  }
}
