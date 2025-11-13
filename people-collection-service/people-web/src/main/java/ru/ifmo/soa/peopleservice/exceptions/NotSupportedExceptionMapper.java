package ru.ifmo.soa.peopleservice.exceptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;

@Provider
@ApplicationScoped
public class NotSupportedExceptionMapper implements ExceptionMapper<NotSupportedException> {
  @Override
  public Response toResponse(NotSupportedException exception) {
    ErrorResponseDto error = new ErrorResponseDto(415, "Content-Type must be application/json");
    return Response.status(415).entity(error).build();
  }
}
