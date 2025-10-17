package ru.ifmo.soa.peopleservice.models;

import java.util.List;

public class FilterCriteria {
    private List<FilterRule> filters;

    public List<FilterRule> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterRule> filters) {
        this.filters = filters;
    }
}
