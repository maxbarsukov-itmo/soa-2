package ru.ifmo.soa.demographyservice;

import jakarta.ws.rs.ApplicationPath;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class Application extends jakarta.ws.rs.core.Application {
  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.add(ru.ifmo.soa.demographyservice.resources.DemographyResource.class);
    classes.add(ru.ifmo.soa.demographyservice.filters.CorsFilter.class);
    classes.add(ru.ifmo.soa.demographyservice.exceptions.GlobalExceptionHandler.class);
    return classes;
  }
}
