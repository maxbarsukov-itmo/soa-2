package ru.ifmo.soa.peopleservice.dto;

import java.io.Serial;
import java.io.Serializable;

public class CallbackError implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public int code;
  public String message;

  public CallbackError(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
