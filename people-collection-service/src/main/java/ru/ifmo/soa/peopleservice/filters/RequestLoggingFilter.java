package ru.ifmo.soa.peopleservice.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import ru.ifmo.soa.peopleservice.models.ErrorResponse;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final long MAX_REQUESTS_PER_MINUTE = 100;
  private static final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  private Bucket createNewBucket() {
    Bandwidth limit = Bandwidth.classic(MAX_REQUESTS_PER_MINUTE, Refill.intervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if ("OPTIONS".equals(requestContext.getMethod())) {
      return;
    }

    String clientIP = requestContext.getHeaderString("X-Forwarded-For");
    if (clientIP == null || clientIP.isEmpty()) {
      clientIP = requestContext.getUriInfo().getRequestUri().getHost();
    }
    Bucket bucket = buckets.computeIfAbsent(clientIP, k -> createNewBucket());

    if (!bucket.tryConsume(1)) {
      requestContext.abortWith(Response.status(jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS)
        .entity(new ErrorResponse(429, "Rate limit exceeded. Please try again later."))
        .build());
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
  }
}
