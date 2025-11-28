package ru.ifmo.soa.ewmalb.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.ifmo.soa.ewmalb.metrics.FallbackMeterRegistry;

@Configuration
public class MetricsConfig {

  private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);

  @Bean
  @Primary
  public MeterRegistry meterRegistry() {
    try {
      CompositeMeterRegistry registry = new CompositeMeterRegistry();
      log.info("Micrometer MeterRegistry initialized successfully");
      return registry;
    } catch (Exception e) {
      log.warn("Failed to initialize Micrometer MeterRegistry, using fallback: {}", e.getMessage());
      return new FallbackMeterRegistry();
    }
  }

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry -> {
      if (!(registry instanceof FallbackMeterRegistry)) {
        registry.config().commonTags(
          "application", "ewma-loadbalancer",
          "version", "1.0.0"
        );
        log.debug("Common tags configured for MeterRegistry");
      }
    };
  }
}
