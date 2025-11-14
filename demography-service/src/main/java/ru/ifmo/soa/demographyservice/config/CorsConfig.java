package ru.ifmo.soa.demographyservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");

    config.addAllowedHeader("origin");
    config.addAllowedHeader("content-type");
    config.addAllowedHeader("accept");
    config.addAllowedHeader("Content-Length");

    config.addAllowedMethod("GET");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("HEAD");

    config.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
}
