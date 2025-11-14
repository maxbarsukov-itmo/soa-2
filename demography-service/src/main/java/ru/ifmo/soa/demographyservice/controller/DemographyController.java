package ru.ifmo.soa.demographyservice.controller;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.demographyservice.service.DemographyService;

@RestController
@RequestMapping("/api/v1/demography")
public class DemographyController {

  private final DemographyService demographyService;

  public DemographyController(DemographyService demographyService) {
    this.demographyService = demographyService;
  }

  @GetMapping("/hair-color/{hairColor}/percentage")
  public Double getHairColorPercentage(@PathVariable @NotBlank String hairColor) {
    return demographyService.getHairColorPercentage(hairColor);
  }

  @GetMapping("/eye-color/{eyeColor}")
  public Long getEyeColorCount(@PathVariable @NotBlank String eyeColor) {
    return demographyService.getEyeColorCount(eyeColor);
  }
}
