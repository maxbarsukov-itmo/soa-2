package ru.ifmo.soa.peopleservice.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import ru.ifmo.soa.peopleservice.dto.FilterCriteriaDto;
import ru.ifmo.soa.peopleservice.dto.FilterRuleDto;
import ru.ifmo.soa.peopleservice.entities.*;
import ru.ifmo.soa.peopleservice.exceptions.NotFoundException;
import ru.ifmo.soa.peopleservice.exceptions.SemanticException;
import ru.ifmo.soa.peopleservice.util.PathResolver;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class PersonRepository {

  private static final int MAX_STORAGE_CAPACITY = 100000;

  @PersistenceContext
  private EntityManager em;

  public boolean isStorageFull() {
    long count = countAll();
    return count >= MAX_STORAGE_CAPACITY;
  }

  public boolean existsSimilarPerson(Person input) {
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

  public List<Person> findAll(int page, int pageSize, PathResolver.SortInfo sortInfo) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Person> cq = cb.createQuery(Person.class);
    Root<Person> root = cq.from(Person.class);
    PathResolver.applySorting(cq, root, cb, sortInfo);
    TypedQuery<Person> query = em.createQuery(cq);
    query.setFirstResult(page * pageSize);
    query.setMaxResults(pageSize);
    return query.getResultList();
  }

  public List<Person> findWithFilters(FilterCriteriaDto criteria, int page, int pageSize, PathResolver.SortInfo sortInfo) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Person> cq = cb.createQuery(Person.class);
    Root<Person> root = cq.from(Person.class);
    Predicate predicate = buildPredicate(root, cb, criteria);
    if (predicate != null) {
      cq.where(predicate);
    }
    PathResolver.applySorting(cq, root, cb, sortInfo);
    TypedQuery<Person> query = em.createQuery(cq);
    query.setFirstResult(page * pageSize);
    query.setMaxResults(pageSize);
    return query.getResultList();
  }

  private Predicate buildPredicate(Root<Person> root, CriteriaBuilder cb, FilterCriteriaDto criteria) {
    if (criteria == null || criteria.getFilters() == null || criteria.getFilters().isEmpty()) {
      return null;
    }
    Predicate overallPredicate = cb.conjunction();
    for (FilterRuleDto rule : criteria.getFilters()) {
      String field = rule.getField();
      String operator = rule.getOperator();
      String value = rule.getValue();
      Path<?> path = PathResolver.getPath(root, field);
      if (path == null) continue;
      Class<?> fieldType = path.getJavaType();
      Object parsedValue = parseValue(fieldType, value);
      Predicate rulePredicate = switch (operator) {
        case "eq" -> cb.equal(path, parsedValue);
        case "ne" -> cb.notEqual(path, parsedValue);
        case "gt" -> cb.greaterThan((Expression<? extends Comparable>) path, (Comparable) parsedValue);
        case "lt" -> cb.lessThan((Expression<? extends Comparable>) path, (Comparable) parsedValue);
        case "gte" -> cb.greaterThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) parsedValue);
        case "lte" -> cb.lessThanOrEqualTo((Expression<? extends Comparable>) path, (Comparable) parsedValue);
        default -> throw new IllegalArgumentException("Unknown operator: " + operator);
      };
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
      } else if (type == OffsetDateTime.class) {
        return OffsetDateTime.parse(value);
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

  public long countWithFilters(FilterCriteriaDto criteria) {
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

  public Person findById(Long id) {
    Person person = em.find(Person.class, id);
    if (person == null) {
      throw new NotFoundException("No person found with the specified ID");
    }
    return person;
  }

  public void save(Person person) {
    em.persist(person);
  }

  public void update(Person person) {
    em.merge(person);
  }

  public void delete(Person person) {
    em.remove(em.contains(person) ? person : em.merge(person));
  }

  public void deleteById(Long id) {
    Person person = em.find(Person.class, id);
    if (person != null) {
      em.remove(person);
    }
  }

  public int deleteByNationality(Country nationality) {
    String jpql = "DELETE FROM Person p WHERE p.nationality = :nationality";
    return em.createQuery(jpql)
      .setParameter("nationality", nationality)
      .executeUpdate();
  }

  public int deleteByLocation(Location location) {
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
    return deletedCount;
  }

  public List<Person> findWithLocationGreaterThan(Integer x, Long y, Integer z) {
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

  public long countAll() {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<Person> root = cq.from(Person.class);
    cq.select(cb.count(root));
    return em.createQuery(cq).getSingleResult();
  }
}
