package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;

@Service
public class ActiveHealthCheckService {

  private static final Logger log = LoggerFactory.getLogger(ActiveHealthCheckService.class);

  private final EwmaLoadBalancer loadBalancer;
  private final CloseableHttpClient httpClient;

  @Value("${ewma.loadbalancer.health-check.enabled:true}")
  private boolean healthCheckEnabled;

  @Value("${ewma.loadbalancer.health-check.path:/health}")
  private String healthCheckPath;

  @Value("${ewma.loadbalancer.health-check.timeout-ms:5000}")
  private int healthCheckTimeout;

  @Value("${ewma.loadbalancer.health-check.interval-ms:30000}")
  private long healthCheckInterval;

  public ActiveHealthCheckService(EwmaLoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
    this.httpClient = HttpClients.createDefault();
  }

  @PostConstruct
  public void init() {
    if (healthCheckEnabled) {
      log.info("Active health checks enabled (path: {}, interval: {}ms, timeout: {}ms)",
        healthCheckPath, healthCheckInterval, healthCheckTimeout);
    }
  }

  @Scheduled(fixedRateString = "${ewma.loadbalancer.health-check.interval-ms:30000}")
  public void performHealthChecks() {
    if (!healthCheckEnabled) return;

    log.debug("Performing active health checks...");

    loadBalancer.getAllInstances().forEach(instance -> {
      String healthUrl = instance.getUrl() + healthCheckPath;

      try {
        long startTime = System.currentTimeMillis();
        HttpGet request = new HttpGet(healthUrl);

        RequestConfig requestConfig = RequestConfig.custom()
          .setConnectionRequestTimeout(Timeout.ofMilliseconds(healthCheckTimeout))
          .setResponseTimeout(Timeout.ofMilliseconds(healthCheckTimeout))
          .build();
        request.setConfig(requestConfig);

        httpClient.execute(request, response -> {
          long responseTime = System.currentTimeMillis() - startTime;
          boolean isHealthy = response.getCode() == 200;

          if (isHealthy) {
            instance.recordSuccess();
            log.debug("Health check passed for {} ({}ms)",
              instance.getId(), responseTime);
          } else {
            instance.recordFailure();
            log.warn("Health check failed for {}: HTTP {}",
              instance.getId(), response.getCode());
          }

          instance.setHealthy(isHealthy);
          return isHealthy;
        });

      } catch (Exception e) {
        log.warn("Health check error for {}: {}", instance.getId(), e.getMessage());
        instance.recordFailure();
        instance.setHealthy(false);
      }
    });

    loadBalancer.logCurrentState();
  }
}
