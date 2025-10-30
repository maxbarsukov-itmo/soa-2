package ru.ifmo.soa.demographyservice;

import jakarta.ws.rs.ApplicationPath;
import ru.ifmo.soa.demographyservice.controller.DemographyController;
import ru.ifmo.soa.demographyservice.exception.GlobalExceptionHandler;
import ru.ifmo.soa.demographyservice.interceptor.CorsFilter;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class Application extends jakarta.ws.rs.core.Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.add(DemographyController.class);
    classes.add(CorsFilter.class);
    classes.add(GlobalExceptionHandler.class);
    return classes;
  }
}
