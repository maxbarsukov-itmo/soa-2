package ru.ifmo.soa.demographyservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PeopleResponseDto(Long totalCount) {}
