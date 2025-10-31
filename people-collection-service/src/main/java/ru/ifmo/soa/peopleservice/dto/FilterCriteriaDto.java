package ru.ifmo.soa.peopleservice.dto;

import java.util.List;

public class FilterCriteriaDto {
  private List<FilterRuleDto> filters;

  public List<FilterRuleDto> getFilters() {
    return filters;
  }

  public void setFilters(List<FilterRuleDto> filters) {
    this.filters = filters;
  }
}
