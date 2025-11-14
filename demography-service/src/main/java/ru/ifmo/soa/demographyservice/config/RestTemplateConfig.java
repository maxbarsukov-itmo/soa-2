package ru.ifmo.soa.demographyservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

  private final ObjectMapper objectMapper;

  public RestTemplateConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  @Primary
  public Jackson2ObjectMapperBuilder objectMapperBuilder() {
    return new Jackson2ObjectMapperBuilder()
      .xml()
      .defaultUseWrapper(false)
      .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  @LoadBalanced
  @Bean
  public RestTemplate restTemplate() {
    try {
      CloseableHttpClient httpClient = HttpClients.custom().build();

      HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

      MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
      jsonConverter.setObjectMapper(objectMapper);
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
      messageConverters.add(jsonConverter);
      messageConverters.add(new StringHttpMessageConverter());

      List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
      interceptors.add(new HeaderRequestInterceptor(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

      RestTemplate restTemplate = new RestTemplate(requestFactory);
      restTemplate.setMessageConverters(messageConverters);
      restTemplate.setInterceptors(interceptors);
      return restTemplate;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create LoadBalanced RestTemplate", e);
    }
  }
}
