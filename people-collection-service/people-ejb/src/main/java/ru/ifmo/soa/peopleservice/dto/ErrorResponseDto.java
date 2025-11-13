package ru.ifmo.soa.peopleservice.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

public class ErrorResponseDto implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Integer code;
  private String message;
  private OffsetDateTime time;

  public ErrorResponseDto(Integer code, String message) {
    this.code = code;
    this.message = message;
    this.time = OffsetDateTime.now();
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public OffsetDateTime getTime() {
    return time;
  }

  public void setTime(OffsetDateTime time) {
    this.time = time;
  }
}
