package ru.ifmo.soa.demographyservice.interceptor;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CorsFilter implements ContainerResponseFilter {

  private static final String ALLOWED_ORIGINS = "*";
  private static final String ALLOWED_METHODS = "GET, OPTIONS, HEAD";
  private static final String ALLOWED_HEADERS = "origin, content-type, accept, Content-Length";

  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) throws IOException {

    responseContext.getHeaders().add("Access-Control-Allow-Origin", ALLOWED_ORIGINS);
    responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
    responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);

    if (isPreflightRequest(requestContext)) {
      responseContext.setStatus(Response.Status.OK.getStatusCode());
    }
  }

  private boolean isPreflightRequest(ContainerRequestContext request) {
    return request.getHeaderString("Origin") != null
      && "OPTIONS".equals(request.getMethod())
      && request.getHeaderString("Access-Control-Request-Method") != null;
  }
}
