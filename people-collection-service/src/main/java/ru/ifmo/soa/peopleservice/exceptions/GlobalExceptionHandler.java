package ru.ifmo.soa.peopleservice.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.peopleservice.models.ErrorResponse;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof BadRequestException) {
      return Response.status(400).entity(new ErrorResponse(400, exception.getMessage())).build();
    } else if (exception instanceof NotFoundException) {
      return Response.status(404).entity(new ErrorResponse(404, exception.getMessage())).build();
    } else if (exception instanceof MethodNotAllowedException) {
      return Response.status(405).entity(new ErrorResponse(405, exception.getMessage())).build();
    } else if (exception instanceof ConflictException) {
      return Response.status(409).entity(new ErrorResponse(409, exception.getMessage())).build();
    } else if (exception instanceof ContentTooLargeException) {
      return Response.status(413).entity(new ErrorResponse(413, exception.getMessage())).build();
    } else if (exception instanceof UnsupportedMediaTypeException) {
      return Response.status(415).entity(new ErrorResponse(415, exception.getMessage())).build();
    } else if (exception instanceof SemanticException) {
      return Response.status(422).entity(new ErrorResponse(422, exception.getMessage())).build();
    } else if (exception instanceof TooManyRequestsException) {
      return Response.status(429).entity(new ErrorResponse(429, exception.getMessage())).build();
    } else if (exception instanceof InsufficientStorageException) {
      return Response.status(507).entity(new ErrorResponse(507, exception.getMessage())).build();
    } else {
      return Response.status(500).entity(new ErrorResponse(500, "Internal server error")).build();
    }
  }
}
