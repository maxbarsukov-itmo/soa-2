package ru.ifmo.soa.peopleservice.exceptions;

public class InsufficientStorageException extends RuntimeException {
  public InsufficientStorageException(String message) {
    super(message);
  }
}
