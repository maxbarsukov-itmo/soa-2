package ru.ifmo.soa.peopleservice.repositories;

import ru.ifmo.soa.peopleservice.config.DatabaseConfig;
import ru.ifmo.soa.peopleservice.entities.Country;
import ru.ifmo.soa.peopleservice.entities.Person;
import ru.ifmo.soa.peopleservice.exceptions.NotFoundException;
import ru.ifmo.soa.peopleservice.exceptions.SemanticException;
import ru.ifmo.soa.peopleservice.models.FilterCriteria;
import ru.ifmo.soa.peopleservice.models.FilterRule;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import ru.ifmo.soa.peopleservice.models.PersonInput;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PersonRepository {

  private static final int MAX_STORAGE_CAPACITY = 100000;

  public boolean isStorageFull() {
    long count = countAll();
    return count >= MAX_STORAGE_CAPACITY;
  }

  public boolean existsSimilarPerson(PersonInput input) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Long> cq = cb.createQuery(Long.class);
      Root<Person> root = cq.from(Person.class);

      Predicate predicate = cb.and(
        cb.equal(root.get("name"), input.getName()),
        cb.equal(root.get("coordinates").get("x"), input.getCoordinates().getX()),
        cb.equal(root.get("coordinates").get("y"), input.getCoordinates().getY()),
        cb.equal(root.get("eyeColor"), input.getEyeColor())
      );

      cq.select(cb.count(root)).where(predicate);
      Long count = em.createQuery(cq).getSingleResult();
      return count > 0;
    }
  }

  public List<Person> findAll(int page, int pageSize, String sortBy, String sortOrder) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Person> cq = cb.createQuery(Person.class);
      Root<Person> root = cq.from(Person.class);

      applySorting(cq, root, cb, sortBy, sortOrder);

      TypedQuery<Person> query = em.createQuery(cq);
      query.setFirstResult(page * pageSize);
      query.setMaxResults(pageSize);
      return query.getResultList();
    }
  }

  public List<Person> findWithFilters(FilterCriteria criteria, int page, int pageSize, String sortBy, String sortOrder) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Person> cq = cb.createQuery(Person.class);
      Root<Person> root = cq.from(Person.class);

      Predicate predicate = buildPredicate(root, cb, criteria);
      if (predicate != null) {
        cq.where(predicate);
      }

      applySorting(cq, root, cb, sortBy, sortOrder);

      TypedQuery<Person> query = em.createQuery(cq);
      query.setFirstResult(page * pageSize);
      query.setMaxResults(pageSize);
      return query.getResultList();
    }
  }

  private Predicate buildPredicate(Root<Person> root, CriteriaBuilder cb, FilterCriteria criteria) {
    if (criteria == null || criteria.getFilters() == null || criteria.getFilters().isEmpty()) {
      return null;
    }

    Predicate overallPredicate = cb.conjunction();

    for (FilterRule rule : criteria.getFilters()) {
      String field = rule.getField();
      String operator = rule.getOperator();
      String value = rule.getValue();

      Path<?> path = getPath(root, field);
      if (path == null) continue;

      Class<?> fieldType = path.getJavaType();

      Object parsedValue = parseValue(fieldType, value);

      Predicate rulePredicate;
      switch (operator) {
        case "eq":
          rulePredicate = cb.equal(path, parsedValue);
          break;
        case "ne":
          rulePredicate = cb.notEqual(path, parsedValue);
          break;
        case "gt":
          rulePredicate = cb.greaterThan((Expression<? extends Comparable>) path, (Comparable) parsedValue);
          break;
        case "lt":
          rulePredicate = cb.lessThan((Expression<? extends Comparable>) path, (Comparable) parsedValue);
          break;
        case "gte":
          rulePredicate = cb.greaterThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) parsedValue);
          break;
        case "lte":
          rulePredicate = cb.lessThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) parsedValue);
          break;
        default:
          throw new IllegalArgumentException("Unknown operator: " + operator);
      }
      overallPredicate = cb.and(overallPredicate, rulePredicate);
    }
    return overallPredicate;
  }

  private Object parseValue(Class<?> type, String value) {
    if (type == null || value == null) return value;

    try {
      if (type == String.class) {
        return value;
      } else if (type == Integer.class || type == int.class) {
        return Integer.valueOf(value);
      } else if (type == Long.class || type == long.class) {
        return Long.valueOf(value);
      } else if (type == Float.class || type == float.class) {
        return Float.valueOf(value);
      } else if (type == Double.class || type == double.class) {
        return Double.valueOf(value);
      } else if (type == Boolean.class || type == boolean.class) {
        return Boolean.parseBoolean(value);
      } else if (type == LocalDateTime.class) {
        return LocalDateTime.parse(value);
      } else if (type.isEnum()) {
        try {
          return Enum.valueOf((Class<Enum>) type, value);
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid enum value: " + value + " for type " + type.getSimpleName());
        }
      }
    } catch (NumberFormatException e) {
      throw new SemanticException("Invalid numeric value: " + value + " for type " + type.getSimpleName());
    } catch (Exception e) {
      throw new SemanticException("Failed to parse value: " + value + " for type " + type.getSimpleName());
    }

    throw new IllegalArgumentException("Unsupported field type: " + type);
  }

  private Path<Object> getPath(Root<Person> root, String field) {
    switch (field) {
      case "id":
        return root.get("id");
      case "name":
        return root.get("name");
      case "creationDate":
        return root.get("creationDate");
      case "coordinates.x":
        return root.get("coordinates").get("x");
      case "coordinates.y":
        return root.get("coordinates").get("y");
      case "height":
        return root.get("height");
      case "eyeColor":
        return root.get("eyeColor");
      case "hairColor":
        return root.get("hairColor");
      case "nationality":
        return root.get("nationality");
      case "location.x":
        return root.get("location").get("x");
      case "location.y":
        return root.get("location").get("y");
      case "location.z":
        return root.get("location").get("z");
      case "location.name":
        return root.get("location").get("name");
      default:
        return null;
    }
  }

  public long countWithFilters(FilterCriteria criteria) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Long> cq = cb.createQuery(Long.class);
      Root<Person> root = cq.from(Person.class);
      Predicate predicate = buildPredicate(root, cb, criteria);
      if (predicate != null) {
        cq.where(predicate);
      }
      cq.select(cb.count(root));
      return em.createQuery(cq).getSingleResult();
    }
  }

  private Object parseValue(Path<Object> path, String value) {
    Class<?> type = path.getJavaType();
    if (type == String.class) {
      return value;
    } else if (type == Integer.class || type == int.class) {
      return Integer.parseInt(value);
    } else if (type == Long.class || type == long.class) {
      return Long.parseLong(value);
    } else if (type == Float.class || type == float.class) {
      return Float.parseFloat(value);
    } else if (type == Boolean.class || type == boolean.class) {
      return Boolean.parseBoolean(value);
    } else {
      return value;
    }
  }

  private void applySorting(CriteriaQuery<Person> cq, Root<Person> root, CriteriaBuilder cb, String sortBy, String sortOrder) {
    if ("id".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("id")));
      } else {
        cq.orderBy(cb.asc(root.get("id")));
      }
    } else if ("name".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("name")));
      } else {
        cq.orderBy(cb.asc(root.get("name")));
      }
    } else if ("creationDate".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("creationDate")));
      } else {
        cq.orderBy(cb.asc(root.get("creationDate")));
      }
    } else if ("coordinates.x".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("coordinates").get("x")));
      } else {
        cq.orderBy(cb.asc(root.get("coordinates").get("x")));
      }
    } else if ("coordinates.y".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("coordinates").get("y")));
      } else {
        cq.orderBy(cb.asc(root.get("coordinates").get("y")));
      }
    } else if ("height".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("height")));
      } else {
        cq.orderBy(cb.asc(root.get("height")));
      }
    } else if ("eyeColor".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("eyeColor")));
      } else {
        cq.orderBy(cb.asc(root.get("eyeColor")));
      }
    } else if ("hairColor".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("hairColor")));
      } else {
        cq.orderBy(cb.asc(root.get("hairColor")));
      }
    } else if ("nationality".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("nationality")));
      } else {
        cq.orderBy(cb.asc(root.get("nationality")));
      }
    } else if ("location.x".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("location").get("x")));
      } else {
        cq.orderBy(cb.asc(root.get("location").get("x")));
      }
    } else if ("location.y".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("location").get("y")));
      } else {
        cq.orderBy(cb.asc(root.get("location").get("y")));
      }
    } else if ("location.z".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("location").get("z")));
      } else {
        cq.orderBy(cb.asc(root.get("location").get("z")));
      }
    } else if ("location.name".equals(sortBy)) {
      if ("desc".equalsIgnoreCase(sortOrder)) {
        cq.orderBy(cb.desc(root.get("location").get("name")));
      } else {
        cq.orderBy(cb.asc(root.get("location").get("name")));
      }
    } else {
      cq.orderBy(cb.asc(root.get("id")));
    }
  }

  public Person findById(Long id) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      Person person = em.find(Person.class, id);
      if (person == null) {
        throw new NotFoundException("No person found with the specified ID");
      }
      return person;
    }
  }

  public void save(Person person) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      em.persist(person);
      em.getTransaction().commit();
    }
  }

  public void update(Person person) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      em.merge(person);
      em.getTransaction().commit();
    }
  }

  public void delete(Person person) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      em.remove(em.contains(person) ? person : em.merge(person));
      em.getTransaction().commit();
    }
  }

  public void deleteById(Long id) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      Person person = em.find(Person.class, id);
      if (person != null) {
        em.remove(person);
      }
      em.getTransaction().commit();
    }
  }

  public int deleteByNationality(Country nationality) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      String jpql = "DELETE FROM Person p WHERE p.nationality = :nationality";
      int deletedCount = em.createQuery(jpql)
        .setParameter("nationality", nationality)
        .executeUpdate();
      em.getTransaction().commit();
      return deletedCount;
    }
  }

  public int deleteByLocation(ru.ifmo.soa.peopleservice.entities.Location location) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      em.getTransaction().begin();
      String findJpql = "SELECT p FROM Person p WHERE p.location.x = :x AND p.location.y = :y AND p.location.z = :z ORDER BY p.id";
      List<Person> people = em.createQuery(findJpql, Person.class)
        .setParameter("x", location.getX())
        .setParameter("y", location.getY())
        .setParameter("z", location.getZ())
        .setMaxResults(1)
        .getResultList();

      int deletedCount = 0;
      if (!people.isEmpty()) {
        em.remove(people.get(0));
        deletedCount = 1;
      }

      em.getTransaction().commit();
      return deletedCount;
    }
  }

  public List<Person> findWithLocationGreaterThan(Integer x, Long y, Integer z) {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Person> cq = cb.createQuery(Person.class);
      Root<Person> root = cq.from(Person.class);
      Predicate predicate = cb.and(
        cb.greaterThan(root.get("location").get("x"), x),
        cb.greaterThan(root.get("location").get("y"), y),
        cb.greaterThan(root.get("location").get("z"), z)
      );
      cq.where(predicate);
      return em.createQuery(cq).getResultList();
    }
  }

  public long countAll() {
    try (EntityManager em = DatabaseConfig.getEntityManagerFactory().createEntityManager()) {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Long> cq = cb.createQuery(Long.class);
      Root<Person> root = cq.from(Person.class);
      cq.select(cb.count(root));
      return em.createQuery(cq).getSingleResult();
    }
  }
}
