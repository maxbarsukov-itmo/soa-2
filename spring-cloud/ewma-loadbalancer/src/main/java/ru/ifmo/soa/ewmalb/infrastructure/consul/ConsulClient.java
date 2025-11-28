package ru.ifmo.soa.ewmalb.infrastructure.consul;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ConsulClient {

  private static final Logger log = LoggerFactory.getLogger(ConsulClient.class);

  private final String consulUrl;
  private final CloseableHttpClient httpClient = HttpClients.createDefault();

  public ConsulClient(@Value("${consul.host}") String host,
                      @Value("${consul.port}") int port) {
    this.consulUrl = "http://" + host + ":" + port;
    log.info("Creating Consul client for {}", consulUrl);
  }

  public ConsulResponse blockingGet(String path, long lastIndex, int blockSeconds) throws IOException {
    String url = consulUrl + path + "?index=" + lastIndex + "&wait=" + blockSeconds + "s";
    HttpGet request = new HttpGet(url);
    return httpClient.execute(request, response -> {
      if (response.getCode() == 200) {
        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        long index = extractConsulIndex(response);
        return new ConsulResponse(body, index);
      } else {
        throw new IOException("Consul returned status: " + response.getCode());
      }
    });
  }

  private long extractConsulIndex(ClassicHttpResponse response) {
    Header[] headers = response.getHeaders("X-Consul-Index");
    if (headers != null && headers.length > 0) {
      try {
        return Long.parseLong(headers[0].getValue());
      } catch (NumberFormatException e) {
        log.warn("Invalid X-Consul-Index header: {}", headers[0].getValue());
      }
    }
    return 1L;
  }
}
