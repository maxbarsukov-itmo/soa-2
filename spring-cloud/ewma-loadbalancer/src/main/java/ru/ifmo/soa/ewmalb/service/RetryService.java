package ru.ifmo.soa.ewmalb.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.config.BackendConfig;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class RetryService {

  private static final Logger log = LoggerFactory.getLogger(RetryService.class);

  private final EwmaLoadBalancer loadBalancer;

  public RetryService(EwmaLoadBalancer loadBalancer) {
    this.loadBalancer = loadBalancer;
  }

  public <T> T executeWithRetry(RetryableOperation<T> operation, String operationName, String service) {
    BackendConfig.BackendSpec spec = loadBalancer.getBackendSpec(service);
    if (!spec.getRetry().isEnabled()) {
      try {
        return operation.execute(service);
      } catch (Exception e) {
        log.error("Operation execution failed (retry disabled)", e);
        throw new RuntimeException(e);
      }
    }

    int maxAttempts = spec.getRetry().getMaxAttempts();

    int attempt = 0;
    Exception lastException = null;

    while (attempt < maxAttempts) {
      attempt++;
      EwmaInstance instance = loadBalancer.selectInstanceForService(service);

      if (instance == null) {
        throw new RuntimeException("No healthy instances available for service: " + service);
      }

      try {
        log.debug("Attempt {}/{} for {} on instance {} (service: {})",
          attempt, maxAttempts, operationName, instance.getId(), service);

        T result = operation.executeWithInstance(instance, service);
        loadBalancer.recordLatency(instance.getId(),
          System.currentTimeMillis() - operation.getStartTime());

        log.debug("{} succeeded for service {} on attempt {}/{}",
          operationName, service, attempt, maxAttempts);
        return result;

      } catch (Exception e) {
        lastException = e;
        loadBalancer.recordFailure(instance.getId());

        log.warn("{} failed for service {} on attempt {}/{} (instance: {}): {}",
          operationName, service, attempt, maxAttempts, instance.getId(), e.getMessage());

        if (attempt < maxAttempts) {
          long backoffMs = calculateBackoff(spec, attempt);
          log.debug("Backing off for {}ms before retry (service: {})", backoffMs, service);
          try {
            Thread.sleep(backoffMs);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ie);
          }
        }
      }
    }

    String message = lastException != null ? lastException.getMessage() : "<no message>";
    throw new RuntimeException("All " + maxAttempts + " attempts failed for " +
      operationName + " (service: " + service + "): " + message, lastException);
  }

  private long calculateBackoff(BackendConfig.BackendSpec spec, int attempt) {
    long baseBackoffMs = spec.getRetry().getBackoffMs();
    long exponentialBackoff = baseBackoffMs * (1L << (attempt - 1));
    long jitter = ThreadLocalRandom.current().nextLong(0, baseBackoffMs / 2);
    return exponentialBackoff + jitter;
  }

  @FunctionalInterface
  public interface RetryableOperation<T> {
    T executeWithInstance(EwmaInstance instance, String service) throws Exception;

    default T execute(String service) throws Exception {
      return executeWithInstance(null, service);
    }

    default long getStartTime() {
      return System.currentTimeMillis();
    }
  }
}
