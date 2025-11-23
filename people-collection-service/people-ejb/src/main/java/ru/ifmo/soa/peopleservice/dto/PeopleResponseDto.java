package ru.ifmo.soa.peopleservice.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "PeopleResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class PeopleResponseDto implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private List<PersonDto> people;
  private Integer page;
  private Integer pageSize;
  private Integer totalPages;
  private Long totalCount;

  public PeopleResponseDto(List<PersonDto> people, Integer page, Integer pageSize, Integer totalPages, Long totalCount) {
    this.people = people;
    this.page = page;
    this.pageSize = pageSize;
    this.totalPages = totalPages;
    this.totalCount = totalCount;
  }

  public List<PersonDto> getPeople() {
    return people;
  }

  public void setPeople(List<PersonDto> people) {
    this.people = people;
  }

  public Integer getPage() {
    return page;
  }

  public void setPage(Integer page) {
    this.page = page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }
}
