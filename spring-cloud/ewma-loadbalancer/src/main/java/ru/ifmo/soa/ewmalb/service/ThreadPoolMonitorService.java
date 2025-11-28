package ru.ifmo.soa.ewmalb.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.Map;
import java.util.HashMap;

@Service
public class ThreadPoolMonitorService {

  private static final Logger log = LoggerFactory.getLogger(ThreadPoolMonitorService.class);

  private final ThreadPoolTaskExecutor loadBalancerExecutor;
  private final ThreadPoolTaskExecutor healthCheckExecutor;
  private final ScheduledExecutorService monitorExecutor;

  public ThreadPoolMonitorService(@Qualifier("loadBalancerTaskExecutor") ThreadPoolTaskExecutor loadBalancerTaskExecutor,
                                  @Qualifier("healthCheckTaskExecutor") ThreadPoolTaskExecutor healthCheckTaskExecutor) {
    this.loadBalancerExecutor = loadBalancerTaskExecutor;
    this.healthCheckExecutor = healthCheckTaskExecutor;
    this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "threadpool-monitor");
      t.setDaemon(true);
      return t;
    });
  }

  @PostConstruct
  public void startMonitoring() {
    monitorExecutor.scheduleAtFixedRate(this::monitorThreadPools, 30, 30, TimeUnit.SECONDS);
    log.info("Thread pool monitoring started");
  }

  private void monitorThreadPools() {
    try {
      monitorLoadBalancerPool();
      monitorHealthCheckPool();
    } catch (Exception e) {
      log.warn("Error monitoring thread pools: {}", e.getMessage());
    }
  }

  private void monitorLoadBalancerPool() {
    ThreadPoolExecutor executor = loadBalancerExecutor.getThreadPoolExecutor();

    int activeCount = executor.getActiveCount();
    long completedCount = executor.getCompletedTaskCount();
    long totalCount = executor.getTaskCount();
    int poolSize = executor.getPoolSize();
    int queueSize = executor.getQueue().size();
    int remainingCapacity = executor.getQueue().remainingCapacity();

    if (activeCount > 0 || queueSize > 0 || poolSize >= executor.getMaximumPoolSize() * 0.8) {
      log.info("LoadBalancer Pool - Active: {}, Queue: {}, Pool: {}/{}, Completed: {}/{}",
        activeCount, queueSize, poolSize, executor.getMaximumPoolSize(),
        completedCount, totalCount);
    }

    if (queueSize > executor.getQueue().remainingCapacity() * 0.8) {
      log.warn("LoadBalancer thread pool queue is nearly full: {}/{}",
        queueSize, queueSize + remainingCapacity);
    }

    if (poolSize >= executor.getMaximumPoolSize() * 0.9) {
      log.warn("LoadBalancer thread pool is near maximum size: {}/{}",
        poolSize, executor.getMaximumPoolSize());
    }
  }

  private void monitorHealthCheckPool() {
    ThreadPoolExecutor executor = healthCheckExecutor.getThreadPoolExecutor();

    int activeCount = executor.getActiveCount();
    int queueSize = executor.getQueue().size();

    if (activeCount > 0 || queueSize > 0) {
      log.debug("HealthCheck Pool - Active: {}, Queue: {}, Pool: {}/{}",
        activeCount, queueSize, executor.getPoolSize(), executor.getMaximumPoolSize());
    }
  }

  public Map<String, Object> getLoadBalancerPoolStats() {
    ThreadPoolExecutor executor = loadBalancerExecutor.getThreadPoolExecutor();
    return getThreadPoolStats(executor, "loadbalancer");
  }

  public Map<String, Object> getHealthCheckPoolStats() {
    ThreadPoolExecutor executor = healthCheckExecutor.getThreadPoolExecutor();
    return getThreadPoolStats(executor, "healthcheck");
  }

  private Map<String, Object> getThreadPoolStats(ThreadPoolExecutor executor, String name) {
    Map<String, Object> stats = new HashMap<>();
    stats.put("name", name);
    stats.put("activeCount", executor.getActiveCount());
    stats.put("poolSize", executor.getPoolSize());
    stats.put("corePoolSize", executor.getCorePoolSize());
    stats.put("maximumPoolSize", executor.getMaximumPoolSize());
    stats.put("largestPoolSize", executor.getLargestPoolSize());
    stats.put("queueSize", executor.getQueue().size());
    stats.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
    stats.put("completedTaskCount", executor.getCompletedTaskCount());
    stats.put("taskCount", executor.getTaskCount());
    stats.put("keepAliveTime", executor.getKeepAliveTime(TimeUnit.SECONDS));

    double utilization = executor.getMaximumPoolSize() > 0 ?
      (double) executor.getActiveCount() / executor.getMaximumPoolSize() * 100 : 0;
    stats.put("utilizationPercent", Math.round(utilization * 100.0) / 100.0);

    return stats;
  }

  public void dynamicScaleUp() {
    ThreadPoolExecutor executor = loadBalancerExecutor.getThreadPoolExecutor();
    int currentMax = executor.getMaximumPoolSize();

    if (currentMax < 500) {
      int newMax = Math.min(currentMax + 50, 500);
      executor.setMaximumPoolSize(newMax);
      log.info("Dynamically scaled thread pool to: {}/{}",
        executor.getCorePoolSize(), newMax);
    }
  }

  public void dynamicScaleDown() {
    ThreadPoolExecutor executor = loadBalancerExecutor.getThreadPoolExecutor();
    int currentMax = executor.getMaximumPoolSize();
    int currentCore = executor.getCorePoolSize();

    if (currentMax > currentCore + 20) {
      int newMax = Math.max(currentMax - 25, currentCore + 10);
      executor.setMaximumPoolSize(newMax);
      log.info("Dynamically scaled down thread pool to: {}/{}",
        currentCore, newMax);
    }
  }
}
