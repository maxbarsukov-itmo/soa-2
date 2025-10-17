package ru.ifmo.soa.demographyservice.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.ifmo.soa.demographyservice.models.EyeColor;
import ru.ifmo.soa.demographyservice.models.HairColor;
import ru.ifmo.soa.demographyservice.exceptions.BadRequestException;
import ru.ifmo.soa.demographyservice.exceptions.NotFoundException;
import ru.ifmo.soa.demographyservice.clients.PeopleServiceClient;

import java.io.IOException;

@Path("/demography")
@Produces(MediaType.APPLICATION_JSON)
public class DemographyResource {

  @GET
  @Path("/hair-color/{hairColor}/percentage")
  public Response getHairColorPercentage(@PathParam("hairColor") String hairColorStr) {
    try {
      HairColor.valueOf(hairColorStr);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid parameters supplied");
    }

    try {
      long countWithColor = PeopleServiceClient.getCountByHairColor(hairColorStr);
      long totalCount = PeopleServiceClient.getTotalCount();

      if (totalCount == 0) {
        throw new NotFoundException("No data available");
      }

      double percentage = (double) countWithColor / totalCount * 100.0;
      return Response.ok(percentage).build();

    } catch (IOException e) {
      throw new RuntimeException("Internal server error", e);
    }
  }

  @GET
  @Path("/eye-color/{eyeColor}")
  public Response getEyeColorCount(@PathParam("eyeColor") String eyeColorStr) {
    try {
      EyeColor.valueOf(eyeColorStr);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid parameters supplied");
    }

    try {
      long count = PeopleServiceClient.getCountByEyeColor(eyeColorStr);
      long totalCount = PeopleServiceClient.getTotalCount();

      if (totalCount == 0) {
        throw new NotFoundException("No data available");
      }

      return Response.ok(count).build();

    } catch (IOException e) {
      throw new RuntimeException("Internal server error", e);
    }
  }
}
