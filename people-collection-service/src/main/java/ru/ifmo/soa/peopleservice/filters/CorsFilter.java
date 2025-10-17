package ru.ifmo.soa.peopleservice.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CorsFilter implements ContainerResponseFilter {

  private static final String ALLOWED_ORIGINS = "*";
  private static final String ALLOWED_METHODS = "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD";
  private static final String ALLOWED_HEADERS = "origin, content-type, accept, authorization, X-Callback-URL, X-Requested-With, Content-Length";
  private static final String ALLOW_CREDENTIALS = "true";
  private static final String MAX_AGE = "1209600";


  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) throws IOException {

    responseContext.getHeaders().add("Access-Control-Allow-Origin", ALLOWED_ORIGINS);
    responseContext.getHeaders().add("Access-Control-Allow-Credentials", ALLOW_CREDENTIALS);
    responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
    responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
    responseContext.getHeaders().add("Access-Control-Max-Age", MAX_AGE);
    responseContext.getHeaders().add("Access-Control-Expose-Headers", "Location, Content-Disposition");

    if (isPreflightRequest(requestContext)) {
      responseContext.setStatus(Response.Status.OK.getStatusCode());
      responseContext.setEntity("");
    }
  }

  private boolean isPreflightRequest(ContainerRequestContext request) {
    return request.getHeaderString("Origin") != null
      && "OPTIONS".equals(request.getMethod())
      && request.getHeaderString("Access-Control-Request-Method") != null;
  }
}
