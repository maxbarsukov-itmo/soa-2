package ru.ifmo.soa.ewmalb.balancer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreaker {

  private final int failureThreshold;
  private final long resetTimeoutMs;
  private final long halfOpenTimeoutMs;

  private final AtomicInteger failures = new AtomicInteger(0);
  private final AtomicLong lastFailureTime = new AtomicLong(0);

  private volatile State state = State.CLOSED;
  private volatile long halfOpenStartTime = 0;

  public CircuitBreaker(int failureThreshold, long resetTimeoutMs, long halfOpenTimeoutMs) {
    this.failureThreshold = failureThreshold;
    this.resetTimeoutMs = resetTimeoutMs;
    this.halfOpenTimeoutMs = halfOpenTimeoutMs;
  }

  public boolean allowRequest() {
    return switch (state) {
      case CLOSED -> true;
      case OPEN -> {
        if (System.currentTimeMillis() - lastFailureTime.get() > resetTimeoutMs) {
          state = State.HALF_OPEN;
          halfOpenStartTime = System.currentTimeMillis();
          yield true;
        }
        yield false;
      }
      case HALF_OPEN -> {
        if (System.currentTimeMillis() - halfOpenStartTime > halfOpenTimeoutMs) {
          state = State.CLOSED;
          failures.set(0);
          yield true;
        }
        yield true;
      }
      default -> false;
    };
  }

  public void recordSuccess() {
    if (state == State.HALF_OPEN) {
      state = State.CLOSED;
      failures.set(0);
    }
  }

  public void recordFailure() {
    failures.incrementAndGet();
    lastFailureTime.set(System.currentTimeMillis());

    if (failures.get() >= failureThreshold) {
      state = State.OPEN;
    }
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public int getFailureCount() {
    return failures.get();
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public long getResetTimeoutMs() {
    return resetTimeoutMs;
  }

  public long getHalfOpenTimeoutMs() {
    return halfOpenTimeoutMs;
  }

  public AtomicInteger getFailures() {
    return failures;
  }

  public AtomicLong getLastFailureTime() {
    return lastFailureTime;
  }

  public long getHalfOpenStartTime() {
    return halfOpenStartTime;
  }

  public enum State {
    CLOSED, OPEN, HALF_OPEN
  }
}
