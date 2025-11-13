package ru.ifmo.soa.peopleservice.exceptions;

public class MethodNotAllowedException extends RuntimeException {
  public MethodNotAllowedException(String message) {
    super(message);
  }
}
