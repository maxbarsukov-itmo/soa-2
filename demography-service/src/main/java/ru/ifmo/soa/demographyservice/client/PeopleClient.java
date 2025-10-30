package ru.ifmo.soa.demographyservice.client;

import java.io.IOException;

public interface PeopleClient {
  long getTotalCount() throws IOException;
  long getCountByField(String field, String value) throws IOException;
}
