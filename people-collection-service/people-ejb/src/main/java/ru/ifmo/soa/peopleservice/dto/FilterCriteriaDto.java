package ru.ifmo.soa.peopleservice.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "FilterCriteria")
@XmlAccessorType(XmlAccessType.FIELD)
public class FilterCriteriaDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private List<FilterRuleDto> filters;

  public List<FilterRuleDto> getFilters() {
    return filters;
  }

  public void setFilters(List<FilterRuleDto> filters) {
    this.filters = filters;
  }
}
