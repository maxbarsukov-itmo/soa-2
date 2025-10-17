package ru.ifmo.soa.demographyservice.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.demographyservice.models.ErrorResponse;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception ex) {
    if (ex instanceof BadRequestException) {
      return Response.status(400).type(MediaType.APPLICATION_JSON).entity(new ErrorResponse(400, ex.getMessage())).build();
    } else if (ex instanceof NotFoundException) {
      return Response.status(404).type(MediaType.APPLICATION_JSON).entity(new ErrorResponse(404, ex.getMessage())).build();
    } else {
      return Response.status(500).type(MediaType.APPLICATION_JSON).entity(new ErrorResponse(500, "Internal server error")).build();
    }
  }
}
