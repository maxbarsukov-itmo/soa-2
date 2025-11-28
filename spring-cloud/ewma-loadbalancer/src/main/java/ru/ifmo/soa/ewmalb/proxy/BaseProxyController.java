package ru.ifmo.soa.ewmalb.proxy;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.service.LatencyDistributionService;
import ru.ifmo.soa.ewmalb.service.MetricsService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;

public abstract class BaseProxyController {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  protected final CloseableHttpClient httpClient;
  protected final MetricsService metricsService;
  protected LatencyDistributionService latencyDistributionService;

  protected BaseProxyController(CloseableHttpClient httpClient, MetricsService metricsService) {
    this.httpClient = httpClient;
    this.metricsService = metricsService;
  }

  public void setLatencyDistributionService(LatencyDistributionService latencyDistributionService) {
    this.latencyDistributionService = latencyDistributionService;
  }

  protected ResponseEntity<byte[]> executeProxyRequest(HttpServletRequest request,
                                                       EwmaInstance instance,
                                                       String service) throws Exception {
    long startTime = System.currentTimeMillis();

    String path = request.getRequestURI().replaceFirst("/proxy/[^/]+", "");
    String qs = request.getQueryString();
    String targetUrl = instance.getUrl() + path + (qs != null ? "?" + qs : "");

    try {
      ClassicHttpRequest httpRequest = createHttpRequest(request, targetUrl);

      return httpClient.execute(httpRequest, response -> {
        long latency = System.currentTimeMillis() - startTime;

        ResponseEntity<byte[]> result = processResponse(response, targetUrl);

        metricsService.recordRequest(instance.getId(), latency, true);
        recordLatency(instance.getId(), latency);

        if (latencyDistributionService != null) {
          latencyDistributionService.recordLatency(instance.getId(), latency);
        }

        return result;
      });

    } catch (Exception e) {
      long latency = System.currentTimeMillis() - startTime;
      metricsService.recordRequest(instance.getId(), latency, false);
      recordFailure(instance.getId());
      throw e;
    }
  }

  protected abstract void recordLatency(String instanceId, long latency);
  protected abstract void recordFailure(String instanceId);

  protected ClassicHttpRequest createHttpRequest(HttpServletRequest request, String targetUrl) throws IOException {
    String method = request.getMethod();
    ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(method)
      .setUri(URI.create(targetUrl));

    copyHeaders(request, requestBuilder);
    copyBody(request, requestBuilder);

    return requestBuilder.build();
  }

  protected ResponseEntity<byte[]> processResponse(ClassicHttpResponse response, String targetUrl) throws IOException {
    HttpHeaders respHeaders = new HttpHeaders();
    for (Header h : response.getHeaders()) {
      respHeaders.add(h.getName(), h.getValue());
    }

    byte[] body;
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      body = EntityUtils.toByteArray(entity);
    } else {
      body = new byte[0];
    }

    int statusCode = response.getCode();
    log.debug("Proxied to {} | status={}", targetUrl, statusCode);

    return new ResponseEntity<>(body, respHeaders, HttpStatus.valueOf(statusCode));
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

    String clientIp = getClientIpAddress(request);
    builder.addHeader("X-Forwarded-For", clientIp);
    builder.addHeader("X-Real-IP", clientIp);
    builder.addHeader("X-Forwarded-By", "ewma-loadbalancer");
  }

  protected boolean isConnectionSensitiveHeader(String name) {
    return "host".equalsIgnoreCase(name)
      || "connection".equalsIgnoreCase(name)
      || "content-length".equalsIgnoreCase(name)
      || "transfer-encoding".equalsIgnoreCase(name)
      || "keep-alive".equalsIgnoreCase(name);
  }

  protected String getClientIpAddress(HttpServletRequest request) {
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

  protected void copyBody(HttpServletRequest request, ClassicRequestBuilder builder) throws IOException {
    String contentTypeHeader = request.getContentType();
    ContentType contentType = contentTypeHeader != null
      ? ContentType.parse(contentTypeHeader)
      : ContentType.APPLICATION_OCTET_STREAM;

    long contentLength = request.getContentLengthLong();
    boolean hasBody = contentLength > 0
      || "chunked".equalsIgnoreCase(request.getHeader("Transfer-Encoding"))
      || (contentLength == -1 && request.getMethod().matches("POST|PUT|PATCH"));

    if (hasBody) {
      InputStream bodyStream = request.getInputStream();
      builder.setEntity(new InputStreamEntity(bodyStream, contentLength, contentType));
    }
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
}
