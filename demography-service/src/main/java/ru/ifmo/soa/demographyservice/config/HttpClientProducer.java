package ru.ifmo.soa.demographyservice.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.IOException;

@ApplicationScoped
public class HttpClientProducer {

  @Inject
  private HttpClientConfig httpClientConfig;

  @Produces
  @ApplicationScoped
  public CloseableHttpClient produceHttpClient() {
    return HttpClients.custom()
      .setConnectionManager(httpClientConfig.createConnectionManager())
      .setDefaultRequestConfig(httpClientConfig.createRequestConfig())
      .build();
  }

  public void disposeHttpClient(@Disposes CloseableHttpClient client) throws IOException {
    client.close();
  }
}
