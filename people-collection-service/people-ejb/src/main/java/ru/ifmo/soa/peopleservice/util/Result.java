package ru.ifmo.soa.peopleservice.util;

import java.io.Serial;
import java.io.Serializable;

public sealed interface Result<T> permits Result.Success, Result.Error {
  record Success<T>(T value) implements Result<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
  }

  record Error<T>(Exception exception) implements Result<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
  }
}
