package ru.ifmo.soa.peopleservice.exceptions;

public class InvalidDataException extends RuntimeException {
  public InvalidDataException(String message) {
    super(message);
  }
}
