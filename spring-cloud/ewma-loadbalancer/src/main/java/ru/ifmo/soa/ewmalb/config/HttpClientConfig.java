package ru.ifmo.soa.ewmalb.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

  @Value("${ewma.httpclient.max-connections-total:200}")
  private int maxConnTotal;

  @Value("${ewma.httpclient.max-connections-per-route:50}")
  private int maxConnPerRoute;

  @Value("${ewma.httpclient.connect-timeout-ms:10000}")
  private int connectTimeoutMs;

  @Value("${ewma.httpclient.socket-timeout-ms:30000}")
  private int socketTimeoutMs;

  @Value("${ewma.httpclient.connection-request-timeout-ms:5000}")
  private int connectionRequestTimeoutMs;

  @Value("${ewma.httpclient.connection-time-to-live-ms:300000}")
  private int connectionTimeToLiveMs;

  @Value("${ewma.httpclient.evict-idle-connections-ms:60000}")
  private int evictIdleConnectionsMs;

  @Value("${ewma.httpclient.evict-expired-connections:true}")
  private boolean evictExpiredConnections;

  @Bean
  public CloseableHttpClient httpClient() {
    PoolingHttpClientConnectionManager connectionManager =
      PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(maxConnTotal)
        .setMaxConnPerRoute(maxConnPerRoute)
        .setDefaultConnectionConfig(ConnectionConfig.custom()
          .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
          .setSocketTimeout(Timeout.ofMilliseconds(socketTimeoutMs))
          .setTimeToLive(Timeout.ofMilliseconds(connectionTimeToLiveMs))
          .build())
        .build();

    RequestConfig requestConfig = RequestConfig.custom()
      .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeoutMs))
      .setResponseTimeout(Timeout.ofMilliseconds(socketTimeoutMs))
      .build();

    HttpClientBuilder builder = HttpClients.custom()
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(requestConfig)
      .setConnectionManagerShared(false)
      .evictExpiredConnections()
      .evictIdleConnections(Timeout.ofMilliseconds(evictIdleConnectionsMs));

    if (evictExpiredConnections) {
      builder
        .evictExpiredConnections()
        .evictIdleConnections(Timeout.ofMilliseconds(evictIdleConnectionsMs));
    }

    return builder.build();
  }
}
