package ru.ifmo.soa.peopleservice;

import jakarta.ws.rs.ApplicationPath;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class Application extends jakarta.ws.rs.core.Application {
  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.add(ru.ifmo.soa.peopleservice.resources.PeopleResource.class);
    classes.add(ru.ifmo.soa.peopleservice.filters.RequestLoggingFilter.class);
    classes.add(ru.ifmo.soa.peopleservice.filters.RequestValidationFilter.class);
    classes.add(ru.ifmo.soa.peopleservice.exceptions.GlobalExceptionHandler.class);
    return classes;
  }
}
