package ru.ifmo.soa.demographyservice.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.demographyservice.dto.ErrorDto;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception ex) {
    if (ex instanceof BadRequestException badRequest) {
      return errorResponse(400, badRequest.getMessage());
    } else if (ex instanceof NotFoundException notFound) {
      return errorResponse(404, notFound.getMessage());
    } else {
      return errorResponse(500, ex.getMessage());
    }
  }

  private Response errorResponse(int status, String message) {
    return Response.status(status)
      .type(MediaType.APPLICATION_JSON)
      .entity(new ErrorDto(status, message))
      .build();
  }
}
