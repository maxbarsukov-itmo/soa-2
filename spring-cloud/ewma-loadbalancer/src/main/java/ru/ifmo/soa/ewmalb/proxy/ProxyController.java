package ru.ifmo.soa.ewmalb.proxy;

import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;
import ru.ifmo.soa.ewmalb.service.*;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.util.Enumeration;
import java.util.List;

@RestController
public class ProxyController extends BaseProxyController {

  private final EwmaLoadBalancer balancer;
  private final RetryService retryService;
  private final StickySessionService stickySessionService;
  private final ABTestingService abTestingService;
  private final RateLimitService rateLimitService;

  public ProxyController(EwmaLoadBalancer balancer,
                         CloseableHttpClient httpClient,
                         RetryService retryService,
                         MetricsService metricsService,
                         StickySessionService stickySessionService,
                         ABTestingService abTestingService,
                         RateLimitService rateLimitService,
                         LatencyDistributionService latencyDistributionService) {
    super(httpClient, metricsService);
    this.balancer = balancer;
    this.retryService = retryService;
    this.stickySessionService = stickySessionService;
    this.abTestingService = abTestingService;
    this.rateLimitService = rateLimitService;
    setLatencyDistributionService(latencyDistributionService);
  }

  @RequestMapping("/proxy/{service}/**")
  public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                      @PathVariable String service) {

    String path = request.getRequestURI();
    if (!rateLimitService.allowRequest(request, path)) {
      return ResponseEntity.status(429)
        .header("X-RateLimit-Limit", "exceeded")
        .body("Rate limit exceeded".getBytes());
    }

    String sessionId = extractSessionId(request);

    return retryService.executeWithRetry((instance, svc) -> {
      EwmaInstance targetInstance = selectTargetInstance(svc, request, sessionId, instance);

      if (targetInstance == null) {
        throw new RuntimeException("No instances available for service: " + service);
      }

      if (sessionId != null) {
        stickySessionService.updateSession(sessionId, service, targetInstance);
      }

      return executeProxyRequest(request, targetInstance, svc);
    }, "proxy request", service);
  }

  private EwmaInstance selectTargetInstance(String service, HttpServletRequest request,
                                            String sessionId, EwmaInstance fallbackInstance) {
    if (abTestingService != null) {
      String experimentId = abTestingService.getExperimentIdForService(service);
      if (experimentId != null) {
        String variant = abTestingService.getVariantForRequest(experimentId, request);

        if (variant != null) {
          List<EwmaInstance> candidates = balancer.getInstancesForService(service);
          EwmaInstance abInstance = abTestingService.getInstanceForVariant(experimentId, variant, candidates);

          if (abInstance != null && abInstance.isHealthy()) {
            log.debug("A/B testing routing for service {}: selected instance {} for variant {}",
              service, abInstance.getId(), variant);
            addABTestingHeaders(request, experimentId, variant);
            return abInstance;
          } else {
            log.warn("A/B testing routing failed for service {}, variant {}, using fallback",
              service, variant);
          }
        }
      }
    }

    if (stickySessionService != null && sessionId != null) {
      EwmaInstance sticky = stickySessionService.getInstanceForService(sessionId, service);
      if (sticky != null) return sticky;
    }

    return fallbackInstance != null ? fallbackInstance : balancer.selectInstanceForService(service);
  }

  private void addABTestingHeaders(HttpServletRequest request, String experimentId, String variant) {
    request.setAttribute("X-AB-Experiment", experimentId);
    request.setAttribute("X-AB-Variant", variant);
  }

  protected void copyHeaders(HttpServletRequest request, ClassicRequestBuilder builder) {
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      if (isConnectionSensitiveHeader(name)) continue;

      Enumeration<String> values = request.getHeaders(name);
      while (values.hasMoreElements()) {
        builder.addHeader(name, values.nextElement());
      }
    }

    Object abExperiment = request.getAttribute("X-AB-Experiment");
    Object abVariant = request.getAttribute("X-AB-Variant");
    if (abExperiment != null && abVariant != null) {
      builder.addHeader("X-AB-Experiment", abExperiment.toString());
      builder.addHeader("X-AB-Variant", abVariant.toString());
    }

    String clientIp = getClientIpAddress(request);
    builder.addHeader("X-Forwarded-For", clientIp);
    builder.addHeader("X-Real-IP", clientIp);
    builder.addHeader("X-Forwarded-By", "ewma-loadbalancer");
  }


  @Override
  protected void recordLatency(String instanceId, long latency) {
    balancer.recordLatency(instanceId, latency);
  }

  @Override
  protected void recordFailure(String instanceId) {
    balancer.recordFailure(instanceId);
  }
}
