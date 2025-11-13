package ru.ifmo.soa.peopleservice.util;

import jakarta.persistence.criteria.*;

public class PathResolver {

  public static <T> Path<Object> getPath(Root<T> root, String field) {
    String[] parts = field.split("\\.");
    Path<Object> path = root.get(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      path = path.get(parts[i]);
    }
    return path;
  }

  public static <T> void applySorting(CriteriaQuery<T> cq, Root<T> root, CriteriaBuilder cb, SortInfo sortInfo) {
    if (sortInfo.getField() == null) {
      cq.orderBy(cb.asc(root.get("id")));
      return;
    }
    Path<Object> path = getPath(root, sortInfo.getField());
    if ("desc".equals(sortInfo.getOrder())) {
      cq.orderBy(cb.desc(path));
    } else {
      cq.orderBy(cb.asc(path));
    }
  }

  public static class SortInfo {
    private final String field;
    private final String order;

    public SortInfo(String field, String order) {
      this.field = field;
      this.order = order != null ? order.toLowerCase() : "asc";
    }

    public String getField() {
      return field;
    }

    public String getOrder() {
      return order;
    }
  }
}
