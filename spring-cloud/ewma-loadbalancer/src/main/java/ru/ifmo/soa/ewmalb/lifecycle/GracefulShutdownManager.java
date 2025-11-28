package ru.ifmo.soa.ewmalb.lifecycle;

import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.service.ActiveHealthCheckService;
import ru.ifmo.soa.ewmalb.service.EnhancedHealthCheckService;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GracefulShutdownManager implements ApplicationListener<ContextClosedEvent> {

  private static final Logger log = LoggerFactory.getLogger(GracefulShutdownManager.class);

  @Value("${ewma.shutdown.timeout-ms:30000}")
  private long shutdownTimeoutMs;

  @Value("${ewma.shutdown.drain-timeout-ms:10000}")
  private long drainTimeoutMs;

  @Value("${ewma.shutdown.health-check-stop-delay-ms:2000}")
  private long healthCheckStopDelayMs;

  @Value("${ewma.shutdown.connection-close-delay-ms:1000}")
  private long connectionCloseDelayMs;

  private final EwmaLoadBalancer loadBalancer;
  private final ActiveHealthCheckService activeHealthCheckService;
  private final EnhancedHealthCheckService enhancedHealthCheckService;
  private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
  private final AtomicBoolean drainMode = new AtomicBoolean(false);
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  public GracefulShutdownManager(EwmaLoadBalancer loadBalancer,
                                 ActiveHealthCheckService activeHealthCheckService,
                                 EnhancedHealthCheckService enhancedHealthCheckService) {
    this.loadBalancer = loadBalancer;
    this.activeHealthCheckService = activeHealthCheckService;
    this.enhancedHealthCheckService = enhancedHealthCheckService;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    initiateGracefulShutdown();
  }

  @PreDestroy
  public void destroy() {
    initiateGracefulShutdown();
  }

  public void initiateGracefulShutdown() {
    if (shuttingDown.compareAndSet(false, true)) {
      log.info("Initiating graceful shutdown (timeout: {}ms)...", shutdownTimeoutMs);

      Thread shutdownThread = new Thread(this::performShutdown, "graceful-shutdown");
      shutdownThread.start();

      try {
        if (!shutdownLatch.await(shutdownTimeoutMs, TimeUnit.MILLISECONDS)) {
          log.warn("Graceful shutdown timeout reached, forcing shutdown");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Shutdown wait interrupted");
      }
    }
  }

  private void performShutdown() {
    try {
      setLoadBalancerDrainMode(true);
      waitForActiveRequestsToComplete();
      stopHealthChecks();
      closeConnections();

      log.info("Graceful shutdown completed successfully");

    } catch (Exception e) {
      log.error("Error during graceful shutdown: {}", e.getMessage());
    } finally {
      shutdownLatch.countDown();
    }
  }

  private void setLoadBalancerDrainMode(boolean drain) {
    log.info("Setting load balancer to {} mode", drain ? "DRAIN" : "ACTIVE");
    drainMode.set(drain);

    if (drain) {
      loadBalancer.getAllInstances().forEach(instance -> {
        log.debug("Marking instance {} as unhealthy for drain mode", instance.getId());
        instance.setHealthy(false);
      });

      setGlobalDrainMode(true);
    } else {
      setGlobalDrainMode(false);
    }
  }

  private void setGlobalDrainMode(boolean drain) {
    try {
      Field drainField = loadBalancer.getClass().getDeclaredField("drainMode");
      drainField.setAccessible(true);
      drainField.set(loadBalancer, drain);
    } catch (Exception e) {
      log.warn("Could not set drain mode flag in load balancer: {}", e.getMessage());
    }
  }

  private void waitForActiveRequestsToComplete() {
    log.info("Waiting for active requests to complete (timeout: {}ms)...", drainTimeoutMs);

    long startTime = System.currentTimeMillis();
    boolean requestsCompleted = false;

    while (System.currentTimeMillis() - startTime < drainTimeoutMs) {
      int totalActiveRequests = loadBalancer.getAllInstances().stream()
        .mapToInt(EwmaInstance::getActiveRequests)
        .sum();

      if (totalActiveRequests == 0) {
        requestsCompleted = true;
        break;
      }

      if (System.currentTimeMillis() - startTime > 1000) {
        log.info("Still waiting for {} active requests to complete... ({}ms elapsed)",
          totalActiveRequests, System.currentTimeMillis() - startTime);
      }

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    if (requestsCompleted) {
      log.info("All active requests completed");
    } else {
      int remainingRequests = loadBalancer.getAllInstances().stream()
        .mapToInt(EwmaInstance::getActiveRequests)
        .sum();
      log.warn("Timeout waiting for active requests to complete. {} requests still active",
        remainingRequests);
    }
  }

  private void stopHealthChecks() {
    log.info("Stopping all health check services...");

    if (enhancedHealthCheckService != null) {
      log.debug("Stopping EnhancedHealthCheckService...");
      stopEnhancedHealthChecks(enhancedHealthCheckService);
    }

    if (enhancedHealthCheckService != null) {
      enhancedHealthCheckService.stopHealthChecks();
    }

    try {
      Thread.sleep(healthCheckStopDelayMs);
      log.info("All health check services stopped");

    } catch (Exception e) {
      log.warn("Error stopping health check services: {}", e.getMessage());
    }
  }

  private void stopEnhancedHealthChecks(EnhancedHealthCheckService healthCheckService) {
    try {
      healthCheckService.setRunning(false);
      healthCheckService.getHttpClient().close();
      log.debug("EnhancedHealthCheckService stopped");
    } catch (Exception e) {
      log.debug("Could not stop EnhancedHealthCheckService: {}", e.getMessage());
    }
  }

  private void closeConnections() {
    log.info("Closing connections...");
    try {
      Thread.sleep(connectionCloseDelayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
    return shutdownLatch.await(timeout, unit);
  }

  public boolean isShuttingDown() {
    return shuttingDown.get();
  }

  public boolean isInDrainMode() {
    return drainMode.get();
  }

  public boolean enableDrainMode() {
    if (!shuttingDown.get()) {
      setLoadBalancerDrainMode(true);
      return true;
    }
    return false;
  }

  public boolean disableDrainMode() {
    if (!shuttingDown.get()) {
      setLoadBalancerDrainMode(false);
      return true;
    }
    return false;
  }
}
