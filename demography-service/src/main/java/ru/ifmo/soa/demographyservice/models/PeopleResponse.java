package ru.ifmo.soa.demographyservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeopleResponse {
  private Long totalCount;

  public Long getTotalCount() { return totalCount; }
  public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
}
