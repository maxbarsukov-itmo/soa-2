package ru.ifmo.soa.ewmalb.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.core.lang.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

public class FallbackMeterRegistry extends MeterRegistry {

  public FallbackMeterRegistry() {
    super(Clock.SYSTEM);
  }

  @Override
  protected <T> Gauge newGauge(Meter.Id id, @Nullable T obj, ToDoubleFunction<T> valueFunction) {
    return new FallbackGauge(id);
  }

  @Override
  protected Counter newCounter(Meter.Id id) {
    return new FallbackCounter(id);
  }

  @Override
  protected Timer newTimer(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, PauseDetector pauseDetector) {
    return new FallbackTimer(id);
  }

  @Override
  protected DistributionSummary newDistributionSummary(Meter.Id id, DistributionStatisticConfig distributionStatisticConfig, double scale) {
    return new FallbackDistributionSummary(id);
  }

  @Override
  protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> measurements) {
    return new FallbackMeter(id, type);
  }

  @Override
  protected <T> FunctionTimer newFunctionTimer(Meter.Id id, T obj, ToLongFunction<T> countFunction, ToDoubleFunction<T> totalTimeFunction, TimeUnit totalTimeFunctionUnit) {
    return null;
  }

  @Override
  protected <T> FunctionCounter newFunctionCounter(Meter.Id id, T obj, ToDoubleFunction<T> countFunction) {
    return null;
  }

  @Override
  protected TimeUnit getBaseTimeUnit() {
    return TimeUnit.MILLISECONDS;
  }

  @Override
  protected DistributionStatisticConfig defaultHistogramConfig() {
    return DistributionStatisticConfig.builder().build();
  }

  private static class FallbackGauge implements Gauge {
    private final Id id;

    FallbackGauge(Id id) {
      this.id = id;
    }

    @Override
    public double value() {
      return 0;
    }

    @Override
    public Id getId() {
      return id;
    }
  }

  private static class FallbackCounter implements Counter {
    private final Id id;

    FallbackCounter(Id id) {
      this.id = id;
    }

    @Override
    public void increment(double amount) {
      // no-op
    }

    @Override
    public double count() {
      return 0;
    }

    @Override
    public Id getId() {
      return id;
    }
  }

  private static class FallbackTimer implements Timer {
    private final Id id;

    FallbackTimer(Id id) {
      this.id = id;
    }

    @Override
    public void record(long amount, TimeUnit unit) {
      // no-op
    }

    @Override
    public void record(Duration duration) {
      Timer.super.record(duration);
    }

    @Override
    public <T> T record(Supplier<T> f) {
      return f.get();
    }

    @Override
    public boolean record(BooleanSupplier f) {
      return Timer.super.record(f);
    }

    @Override
    public int record(IntSupplier f) {
      return Timer.super.record(f);
    }

    @Override
    public long record(LongSupplier f) {
      return Timer.super.record(f);
    }

    @Override
    public double record(DoubleSupplier f) {
      return Timer.super.record(f);
    }

    @Override
    public <T> T recordCallable(Callable<T> f) throws Exception {
      return f.call();
    }

    @Override
    public void record(Runnable f) {
      f.run();
    }

    @Override
    public Runnable wrap(Runnable f) {
      return Timer.super.wrap(f);
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> f) {
      return Timer.super.wrap(f);
    }

    @Override
    public <T> Supplier<T> wrap(Supplier<T> f) {
      return Timer.super.wrap(f);
    }

    @Override
    public long count() {
      return 0;
    }

    @Override
    public double totalTime(TimeUnit unit) {
      return 0;
    }

    @Override
    public double mean(TimeUnit unit) {
      return Timer.super.mean(unit);
    }

    @Override
    public double max(TimeUnit unit) {
      return 0;
    }

    @Override
    public TimeUnit baseTimeUnit() {
      return TimeUnit.MILLISECONDS;
    }

    @Override
    public Id getId() {
      return id;
    }

    @Override
    public Iterable<Measurement> measure() {
      return Collections.emptyList();
    }

    @Override
    public <T> T match(Function<Gauge, T> visitGauge, Function<Counter, T> visitCounter, Function<Timer, T> visitTimer, Function<DistributionSummary, T> visitSummary, Function<LongTaskTimer, T> visitLongTaskTimer, Function<TimeGauge, T> visitTimeGauge, Function<FunctionCounter, T> visitFunctionCounter, Function<FunctionTimer, T> visitFunctionTimer, Function<Meter, T> visitMeter) {
      return Timer.super.match(visitGauge, visitCounter, visitTimer, visitSummary, visitLongTaskTimer, visitTimeGauge, visitFunctionCounter, visitFunctionTimer, visitMeter);
    }

    @Override
    public void use(Consumer<Gauge> visitGauge, Consumer<Counter> visitCounter, Consumer<Timer> visitTimer, Consumer<DistributionSummary> visitSummary, Consumer<LongTaskTimer> visitLongTaskTimer, Consumer<TimeGauge> visitTimeGauge, Consumer<FunctionCounter> visitFunctionCounter, Consumer<FunctionTimer> visitFunctionTimer, Consumer<Meter> visitMeter) {
      Timer.super.use(visitGauge, visitCounter, visitTimer, visitSummary, visitLongTaskTimer, visitTimeGauge, visitFunctionCounter, visitFunctionTimer, visitMeter);
    }

    @Override
    public void close() {
      Timer.super.close();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
      return null;
    }
  }

  private static class FallbackDistributionSummary implements DistributionSummary {
    private final Id id;

    FallbackDistributionSummary(Id id) {
      this.id = id;
    }

    @Override
    public void record(double amount) {
      // no-op
    }

    @Override
    public long count() {
      return 0;
    }

    @Override
    public double totalAmount() {
      return 0;
    }

    @Override
    public double mean() {
      return DistributionSummary.super.mean();
    }

    @Override
    public double max() {
      return 0;
    }

    @Override
    public Id getId() {
      return id;
    }

    @Override
    public Iterable<Measurement> measure() {
      return Collections.emptyList();
    }

    @Override
    public <T> T match(Function<Gauge, T> visitGauge, Function<Counter, T> visitCounter, Function<Timer, T> visitTimer, Function<DistributionSummary, T> visitSummary, Function<LongTaskTimer, T> visitLongTaskTimer, Function<TimeGauge, T> visitTimeGauge, Function<FunctionCounter, T> visitFunctionCounter, Function<FunctionTimer, T> visitFunctionTimer, Function<Meter, T> visitMeter) {
      return DistributionSummary.super.match(visitGauge, visitCounter, visitTimer, visitSummary, visitLongTaskTimer, visitTimeGauge, visitFunctionCounter, visitFunctionTimer, visitMeter);
    }

    @Override
    public void use(Consumer<Gauge> visitGauge, Consumer<Counter> visitCounter, Consumer<Timer> visitTimer, Consumer<DistributionSummary> visitSummary, Consumer<LongTaskTimer> visitLongTaskTimer, Consumer<TimeGauge> visitTimeGauge, Consumer<FunctionCounter> visitFunctionCounter, Consumer<FunctionTimer> visitFunctionTimer, Consumer<Meter> visitMeter) {
      DistributionSummary.super.use(visitGauge, visitCounter, visitTimer, visitSummary, visitLongTaskTimer, visitTimeGauge, visitFunctionCounter, visitFunctionTimer, visitMeter);
    }

    @Override
    public void close() {
      DistributionSummary.super.close();
    }

    @Override
    public HistogramSnapshot takeSnapshot() {
      return null;
    }
  }

  private static class FallbackMeter implements Meter {
    private final Id id;
    private final Type type;

    FallbackMeter(Id id, Type type) {
      this.id = id;
      this.type = type;
    }

    @Override
    public Id getId() {
      return id;
    }

    @Override
    public List<Measurement> measure() {
      return Collections.emptyList();
    }
  }
}
