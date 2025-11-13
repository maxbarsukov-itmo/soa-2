package ru.ifmo.soa.peopleservice.exceptions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.peopleservice.dto.ErrorResponseDto;

@Provider
@ApplicationScoped
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof BadRequestException) {
      return Response.status(400).entity(new ErrorResponseDto(400, exception.getMessage())).build();
    } else if (exception instanceof NotFoundException) {
      return Response.status(404).entity(new ErrorResponseDto(404, exception.getMessage())).build();
    } else if (exception instanceof MethodNotAllowedException) {
      return Response.status(405).entity(new ErrorResponseDto(405, exception.getMessage())).build();
    } else if (exception instanceof ConflictException) {
      return Response.status(409).entity(new ErrorResponseDto(409, exception.getMessage())).build();
    } else if (exception instanceof ContentTooLargeException) {
      return Response.status(413).entity(new ErrorResponseDto(413, exception.getMessage())).build();
    } else if (exception instanceof UnsupportedMediaTypeException) {
      return Response.status(415).entity(new ErrorResponseDto(415, exception.getMessage())).build();
    } else if (exception instanceof SemanticException) {
      return Response.status(422).entity(new ErrorResponseDto(422, exception.getMessage())).build();
    } else if (exception instanceof TooManyRequestsException) {
      return Response.status(429).entity(new ErrorResponseDto(429, exception.getMessage())).build();
    } else if (exception instanceof InsufficientStorageException) {
      return Response.status(507).entity(new ErrorResponseDto(507, exception.getMessage())).build();
    } else {
       return Response.status(500).entity(new ErrorResponseDto(500, "Internal server error")).build();
    }
  }
}
