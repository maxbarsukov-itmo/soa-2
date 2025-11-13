package ru.ifmo.soa.peopleservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import java.text.SimpleDateFormat;

@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

  private final ObjectMapper mapper;

  public JacksonConfig() {
    this.mapper = new ObjectMapper();

    JavaTimeModule javaTimeModule = new JavaTimeModule();
    mapper.registerModule(javaTimeModule);

    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
  }

  @Override
  public ObjectMapper getContext(Class<?> type) {
    return mapper;
  }
}
