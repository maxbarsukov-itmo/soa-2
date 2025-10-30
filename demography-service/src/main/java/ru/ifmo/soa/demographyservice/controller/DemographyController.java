package ru.ifmo.soa.demographyservice.controller;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.demographyservice.service.DemographyService;

@Path("/demography")
@Produces(MediaType.APPLICATION_JSON)
public class DemographyController {

  @Inject
  private DemographyService demographyService;

  @GET
  @Path("/hair-color/{hairColor}/percentage")
  public Response getHairColorPercentage(@PathParam("hairColor") @NotBlank String hairColor) {
    double percentage = demographyService.getHairColorPercentage(hairColor);
    return Response.ok(percentage).build();
  }

  @GET
  @Path("/eye-color/{eyeColor}")
  public Response getEyeColorCount(@PathParam("eyeColor") @NotBlank String eyeColor) {
    long count = demographyService.getEyeColorCount(eyeColor);
    return Response.ok(count).build();
  }
}
