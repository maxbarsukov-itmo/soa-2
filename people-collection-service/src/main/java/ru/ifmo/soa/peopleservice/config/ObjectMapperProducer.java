package ru.ifmo.soa.peopleservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.text.SimpleDateFormat;

@ApplicationScoped
public class ObjectMapperProducer {

  @Produces
  public ObjectMapper objectMapper() {
    var mapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    mapper.registerModule(javaTimeModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
    return mapper;
  }
}
