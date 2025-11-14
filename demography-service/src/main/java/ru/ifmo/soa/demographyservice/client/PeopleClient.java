package ru.ifmo.soa.demographyservice.client;

public interface PeopleClient {
  long getTotalCount();
  long getCountByField(String field, String value);
}
