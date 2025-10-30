package ru.ifmo.soa.demographyservice.client.model;

import java.util.List;

public record PeopleSearchRequest(List<FilterRule> filters) {
  public record FilterRule(String field, String operator, String value) {}
}
