package ru.ifmo.soa.demographyservice.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

  @Value("${http-client.connect-timeout:5}")
  private int connectTimeoutSec;

  @Value("${http-client.response-timeout:10}")
  private int responseTimeoutSec;

  @Value("${http-client.max-total:10}")
  private int maxTotal;

  @Value("${http-client.max-per-route:5}")
  private int maxPerRoute;

  @Bean
  public CloseableHttpClient httpClient() {
    ConnectionConfig connConfig = ConnectionConfig.custom()
      .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSec))
      .build();

    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    connManager.setDefaultConnectionConfig(connConfig);
    connManager.setMaxTotal(maxTotal);
    connManager.setDefaultMaxPerRoute(maxPerRoute);

    RequestConfig requestConfig = RequestConfig.custom()
      .setResponseTimeout(Timeout.ofSeconds(responseTimeoutSec))
      .build();

    return HttpClients.custom()
      .setConnectionManager(connManager)
      .setDefaultRequestConfig(requestConfig)
      .build();
  }
}
