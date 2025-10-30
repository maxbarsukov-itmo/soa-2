package ru.ifmo.soa.demographyservice.dto;

import java.time.OffsetDateTime;

public record ErrorDto(
  Integer code,
  String message,
  OffsetDateTime time
) {
  public ErrorDto(Integer code, String message) {
    this(code, message, OffsetDateTime.now());
  }
}
