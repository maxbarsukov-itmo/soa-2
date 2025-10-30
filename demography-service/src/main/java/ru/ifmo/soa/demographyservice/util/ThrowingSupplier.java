package ru.ifmo.soa.demographyservice.util;

import java.io.IOException;

@FunctionalInterface
public interface ThrowingSupplier<T> {
  T get() throws IOException;
}
