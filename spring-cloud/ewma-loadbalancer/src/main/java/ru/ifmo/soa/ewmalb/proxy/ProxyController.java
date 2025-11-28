package ru.ifmo.soa.ewmalb.proxy;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import ru.ifmo.soa.ewmalb.balancer.EwmaInstance;
import ru.ifmo.soa.ewmalb.balancer.EwmaLoadBalancer;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@RestController
public class ProxyController {

  private record BackendResponse(int statusCode, HttpHeaders headers, byte[] body) {}

  private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

  private final EwmaLoadBalancer balancer;
  private final CloseableHttpClient httpClient;

  public ProxyController(EwmaLoadBalancer balancer) {
    this.balancer = balancer;

    ConnectionConfig connectionConfig = ConnectionConfig.custom()
      .setConnectTimeout(Timeout.ofSeconds(10))
      .setTimeToLive(TimeValue.ofMinutes(5))
      .build();

    RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(Timeout.ofSeconds(10))
      .setResponseTimeout(Timeout.ofSeconds(30))
      .build();

    this.httpClient = HttpClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(20)
        .setDefaultConnectionConfig(connectionConfig)
        .build())
      .build();
  }

  @RequestMapping("/proxy/{service}/**")
  public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                      @PathVariable String service) throws Exception {

    EwmaInstance backend = balancer.selectInstance();
    if (backend == null) {
      throw new RuntimeException("No healthy backend for service: " + service);
    }

    String path = request.getRequestURI().replaceFirst("/proxy/[^/]+", "");
    String qs = request.getQueryString();
    String targetUrl = backend.getUrl() + path + (qs != null ? "?" + qs : "");

    long start = System.currentTimeMillis();

    try {
      String method = request.getMethod();
      ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(method).setUri(URI.create(targetUrl));

      copyHeaders(request, requestBuilder);
      copyBody(request, requestBuilder);

      ClassicHttpRequest httpRequest = requestBuilder.build();

      BackendResponse backendResponse = httpClient.execute(httpRequest, response -> {
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

        if (log.isDebugEnabled()) {
          log.debug("Proxying request to {} with body: {}", targetUrl,
            body == null ? "none" : new String(body, StandardCharsets.UTF_8));
        }

        return new BackendResponse(response.getCode(), respHeaders, body);
      });

      long rtt = System.currentTimeMillis() - start;
      balancer.recordLatency(backend.getId(), rtt);
      log.info("→ {} | status={} | rtt={}ms", targetUrl, backendResponse.statusCode(), rtt);

      return new ResponseEntity<>(
        backendResponse.body(),
        backendResponse.headers(),
        HttpStatus.valueOf(backendResponse.statusCode())
      );

    } catch (Exception e) {
      long rtt = System.currentTimeMillis() - start;
      log.warn("→ {} | FAILED after {}ms: {}", targetUrl, rtt, e.getMessage());
      throw e;
    }
  }

  private void copyHeaders(HttpServletRequest request, ClassicRequestBuilder builder) {
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      if (isConnectionSensitiveHeader(name)) continue;
      if ("x-forwarded-for".equalsIgnoreCase(name) || "x-real-ip".equalsIgnoreCase(name)) {
        continue;
      }

      Enumeration<String> values = request.getHeaders(name);
      while (values.hasMoreElements()) {
        builder.addHeader(name, values.nextElement());
      }
    }

    String clientIp = getClientIpAddress(request);
    builder.addHeader("X-Forwarded-For", clientIp);
    builder.addHeader("X-Real-IP", clientIp);
  }

  private boolean isConnectionSensitiveHeader(String name) {
    return "host".equalsIgnoreCase(name)
      || "connection".equalsIgnoreCase(name)
      || "content-length".equalsIgnoreCase(name)
      || "transfer-encoding".equalsIgnoreCase(name)
      || "keep-alive".equalsIgnoreCase(name);
  }

  private String getClientIpAddress(HttpServletRequest request) {
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

  private void copyBody(HttpServletRequest request, ClassicRequestBuilder builder) throws IOException {
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
}
