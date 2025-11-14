package ru.ifmo.soa.demographyservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration
public class ObjectMapperProducer {

  @Bean
  public ObjectMapper objectMapper() {
    var mapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    mapper.registerModule(javaTimeModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    return mapper;
  }
}
