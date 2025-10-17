package ru.ifmo.soa.peopleservice.exceptions;

public class CallbackError {
  public int code;
  public String message;
  public CallbackError(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
