package ru.ifmo.soa.peopleservice.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class FilterCriteriaDto implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<FilterRuleDto> filters;

  public List<FilterRuleDto> getFilters() {
    return filters;
  }

  public void setFilters(List<FilterRuleDto> filters) {
    this.filters = filters;
  }
}
