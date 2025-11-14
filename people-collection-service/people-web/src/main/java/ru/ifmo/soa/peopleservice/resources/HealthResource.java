package ru.ifmo.soa.peopleservice.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("/health")
public class HealthResource {

  private static final Logger LOG = Logger.getLogger(HealthResource.class.getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response health() {
    LOG.info("Health check started");
    return Response.ok().entity("{\"status\":\"UP\"}").build();
  }
}
