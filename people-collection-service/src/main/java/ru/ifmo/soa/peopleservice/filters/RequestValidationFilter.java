package ru.ifmo.soa.peopleservice.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.soa.peopleservice.models.ErrorResponse;

import java.io.IOException;
import java.time.OffsetDateTime;

@Provider
public class RequestValidationFilter implements ContainerRequestFilter {

  private static final long MAX_CONTENT_LENGTH = 1024 * 1024; // 1 MB

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String method = requestContext.getMethod();
    if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
      String contentType = requestContext.getHeaderString("Content-Type");
      if (contentType == null) {
        ErrorResponse error = new ErrorResponse(415, "Content-Type header is required");
        error.setTime(OffsetDateTime.now());
        requestContext.abortWith(Response.status(415).entity(error).build());
        return;
      }

      contentType = contentType.toLowerCase();
      if (!contentType.startsWith("application/json")) {
        ErrorResponse error = new ErrorResponse(415, "Content-Type must be application/json");
        error.setTime(OffsetDateTime.now());
        requestContext.abortWith(Response.status(415).entity(error).build());
        return;
      }
    }

    String contentType = requestContext.getHeaderString("Content-Type");
    if (contentType != null) {
      contentType = contentType.toLowerCase();
      if (!contentType.startsWith("application/json")) {
        ErrorResponse error = new ErrorResponse(415, "Content-Type must be application/json");
        error.setTime(OffsetDateTime.now());
        requestContext.abortWith(Response.status(415).entity(error).build());
        return;
      }
    }

    String contentLengthHeader = requestContext.getHeaderString("Content-Length");
    if (contentLengthHeader != null) {
      try {
        long contentLength = Long.parseLong(contentLengthHeader.trim());
        if (contentLength > MAX_CONTENT_LENGTH) {
          ErrorResponse error = new ErrorResponse(413, "Request entity is too large");
          error.setTime(OffsetDateTime.now());
          requestContext.abortWith(Response.status(413).entity(error).build());
        }
      } catch (NumberFormatException ignored) {
      }
    }
  }
}
