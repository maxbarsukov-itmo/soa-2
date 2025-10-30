package ru.ifmo.soa.demographyservice.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ru.ifmo.soa.demographyservice.client.PeopleApiClient;
import ru.ifmo.soa.demographyservice.exception.NotFoundException;
import ru.ifmo.soa.demographyservice.model.DemographyField;
import ru.ifmo.soa.demographyservice.model.EyeColor;
import ru.ifmo.soa.demographyservice.model.HairColor;
import ru.ifmo.soa.demographyservice.util.ThrowingSupplier;

import java.io.IOException;
import java.util.function.BiFunction;

@ApplicationScoped
public class DemographyService {

  @Inject
  private PeopleApiClient peopleClient;

  @Inject
  private ValidationService validationService;

  public double getHairColorPercentage(String hairColorStr) {
    var color = validationService.validateEnum(HairColor.class, hairColorStr, DemographyField.HAIR_COLOR.apiName);
    return withTotalCount(
      () -> peopleClient.getCountByField(DemographyField.HAIR_COLOR.apiName, color.name()),
      (count, total) -> (double) count / total * 100.0
    );
  }

  public long getEyeColorCount(String eyeColorStr) {
    var color = validationService.validateEnum(EyeColor.class, eyeColorStr, DemographyField.EYE_COLOR.apiName);
    return withTotalCount(
      () -> peopleClient.getCountByField(DemographyField.EYE_COLOR.apiName, color.name()),
      (count, total) -> count
    );
  }

  private <T> T withTotalCount(ThrowingSupplier<Long> countSupplier, BiFunction<Long, Long, T> resultMapper) {
    try {
      long total = peopleClient.getTotalCount();
      if (total == 0) {
        throw new NotFoundException("No data available");
      }
      long count = countSupplier.get();
      return resultMapper.apply(count, total);
    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch demographic data", e);
    }
  }
}
