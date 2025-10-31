package ru.ifmo.soa.peopleservice.exceptions;

public class UnsupportedMediaTypeException extends RuntimeException {
  public UnsupportedMediaTypeException(String message) {
    super(message);
  }
}
