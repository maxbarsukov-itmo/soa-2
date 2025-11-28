package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

@Service
public class LatencyDistributionService {

  private static final Logger log = LoggerFactory.getLogger(LatencyDistributionService.class);

  private final ConcurrentHashMap<String, LatencyHistogram> instanceHistograms =
    new ConcurrentHashMap<>();

  private static final long[] LATENCY_BUCKETS = {10, 25, 50, 100, 250, 500, 1000, 2500, 5000};

  @PostConstruct
  public void init() {
    log.info("Latency distribution tracking initialized with buckets: {}",
      Arrays.toString(LATENCY_BUCKETS));
  }

  public void recordLatency(String instanceId, long latencyMs) {
    LatencyHistogram histogram = instanceHistograms.computeIfAbsent(instanceId,
      id -> new LatencyHistogram());

    histogram.recordLatency(latencyMs);
  }

  public Map<String, Object> getLatencyDistribution(String instanceId) {
    LatencyHistogram histogram = instanceHistograms.get(instanceId);
    if (histogram == null) {
      return Collections.emptyMap();
    }

    return histogram.getDistribution();
  }

  public Map<String, Object> getAllDistributions() {
    Map<String, Object> distributions = new HashMap<>();

    instanceHistograms.forEach((instanceId, histogram) -> {
      distributions.put(instanceId, histogram.getDistribution());
    });

    return distributions;
  }

  public Map<String, Object> getPercentiles(String instanceId) {
    LatencyHistogram histogram = instanceHistograms.get(instanceId);
    if (histogram == null) {
      return Collections.emptyMap();
    }

    return histogram.getPercentiles();
  }

  public void resetDistribution(String instanceId) {
    instanceHistograms.remove(instanceId);
  }

  private static class LatencyHistogram {
    private final AtomicLong[] bucketCounts;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final List<Long> recentLatencies = new ArrayList<>();
    private static final int RECENT_SAMPLES = 1000;

    public LatencyHistogram() {
      this.bucketCounts = new AtomicLong[LATENCY_BUCKETS.length + 1];
      for (int i = 0; i < bucketCounts.length; i++) {
        bucketCounts[i] = new AtomicLong(0);
      }
    }

    public void recordLatency(long latencyMs) {
      totalRequests.incrementAndGet();
      totalLatency.addAndGet(latencyMs);

      int bucketIndex = findBucketIndex(latencyMs);
      bucketCounts[bucketIndex].incrementAndGet();

      synchronized (recentLatencies) {
        recentLatencies.add(latencyMs);
        if (recentLatencies.size() > RECENT_SAMPLES) {
          recentLatencies.remove(0);
        }
      }
    }

    private int findBucketIndex(long latencyMs) {
      for (int i = 0; i < LATENCY_BUCKETS.length; i++) {
        if (latencyMs <= LATENCY_BUCKETS[i]) {
          return i;
        }
      }
      return LATENCY_BUCKETS.length;
    }

    public Map<String, Object> getDistribution() {
      Map<String, Object> distribution = new LinkedHashMap<>();
      long total = totalRequests.get();

      for (int i = 0; i < LATENCY_BUCKETS.length; i++) {
        long count = bucketCounts[i].get();
        double percentage = total > 0 ? (double) count / total * 100 : 0;

        String bucketName = (i == 0) ?
          "≤" + LATENCY_BUCKETS[i] + "ms" :
          LATENCY_BUCKETS[i-1] + "-" + LATENCY_BUCKETS[i] + "ms";

        distribution.put(bucketName, Map.of(
          "count", count,
          "percentage", Math.round(percentage * 100.0) / 100.0
        ));
      }

      long overflowCount = bucketCounts[LATENCY_BUCKETS.length].get();
      double overflowPercentage = total > 0 ? (double) overflowCount / total * 100 : 0;
      distribution.put(">" + LATENCY_BUCKETS[LATENCY_BUCKETS.length - 1] + "ms", Map.of(
        "count", overflowCount,
        "percentage", Math.round(overflowPercentage * 100.0) / 100.0
      ));

      distribution.put("total_requests", total);
      distribution.put("average_latency", total > 0 ?
        Math.round((double) totalLatency.get() / total * 100.0) / 100.0 : 0);

      return distribution;
    }

    public Map<String, Object> getPercentiles() {
      List<Long> samples;
      synchronized (recentLatencies) {
        samples = new ArrayList<>(recentLatencies);
      }

      if (samples.isEmpty()) {
        return Collections.emptyMap();
      }

      Collections.sort(samples);

      Map<String, Object> percentiles = new LinkedHashMap<>();
      percentiles.put("p50", calculatePercentile(samples, 50));
      percentiles.put("p75", calculatePercentile(samples, 75));
      percentiles.put("p90", calculatePercentile(samples, 90));
      percentiles.put("p95", calculatePercentile(samples, 95));
      percentiles.put("p99", calculatePercentile(samples, 99));
      percentiles.put("sample_size", samples.size());

      return percentiles;
    }

    private long calculatePercentile(List<Long> sortedSamples, double percentile) {
      int index = (int) Math.ceil(percentile / 100.0 * sortedSamples.size()) - 1;
      index = Math.max(0, Math.min(index, sortedSamples.size() - 1));
      return sortedSamples.get(index);
    }
  }
}
