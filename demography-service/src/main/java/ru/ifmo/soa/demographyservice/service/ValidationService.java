package ru.ifmo.soa.demographyservice.service;

import org.springframework.stereotype.Service;
import ru.ifmo.soa.demographyservice.exception.BadRequestException;

@Service
public class ValidationService {

  public <T extends Enum<T>> T validateEnum(Class<T> enumClass, String value, String fieldName) {
    try {
      return Enum.valueOf(enumClass, value);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid " + fieldName + ": " + value);
    }
  }
}
