package ru.ifmo.soa.ewmalb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

  private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

  @Value("${ewma.threadpool.loadbalancer.core-size:50}")
  private int loadBalancerCoreSize;

  @Value("${ewma.threadpool.loadbalancer.max-size:200}")
  private int loadBalancerMaxSize;

  @Value("${ewma.threadpool.loadbalancer.queue-capacity:1000}")
  private int loadBalancerQueueCapacity;

  @Value("${ewma.threadpool.loadbalancer.keep-alive-seconds:60}")
  private int loadBalancerKeepAliveSeconds;

  @Value("${ewma.threadpool.loadbalancer.await-termination-seconds:30}")
  private int loadBalancerAwaitTerminationSeconds;

  @Value("${ewma.threadpool.healthcheck.core-size:10}")
  private int healthCheckCoreSize;

  @Value("${ewma.threadpool.healthcheck.max-size:20}")
  private int healthCheckMaxSize;

  @Value("${ewma.threadpool.healthcheck.queue-capacity:100}")
  private int healthCheckQueueCapacity;

  @Value("${ewma.threadpool.healthcheck.keep-alive-seconds:30}")
  private int healthCheckKeepAliveSeconds;

  @Value("${ewma.threadpool.healthcheck.await-termination-seconds:10}")
  private int healthCheckAwaitTerminationSeconds;

  @Bean(name = "loadBalancerTaskExecutor")
  public ThreadPoolTaskExecutor loadBalancerTaskExecutor() {
    return createThreadPoolExecutor(
      loadBalancerCoreSize,
      loadBalancerMaxSize,
      loadBalancerQueueCapacity,
      loadBalancerKeepAliveSeconds,
      loadBalancerAwaitTerminationSeconds,
      "ewma-lb-",
      new ThreadPoolExecutor.CallerRunsPolicy()
    );
  }

  @Bean(name = "healthCheckTaskExecutor")
  public ThreadPoolTaskExecutor healthCheckTaskExecutor() {
    return createThreadPoolExecutor(
      healthCheckCoreSize,
      healthCheckMaxSize,
      healthCheckQueueCapacity,
      healthCheckKeepAliveSeconds,
      healthCheckAwaitTerminationSeconds,
      "ewma-health-",
      new ThreadPoolExecutor.DiscardPolicy()
    );
  }

  private ThreadPoolTaskExecutor createThreadPoolExecutor(
    int coreSize, int maxSize, int queueCapacity, int keepAliveSeconds,
    int awaitTerminationSeconds, String threadNamePrefix,
    RejectedExecutionHandler rejectionPolicy) {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(coreSize);
    executor.setMaxPoolSize(maxSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setKeepAliveSeconds(keepAliveSeconds);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.setRejectedExecutionHandler(rejectionPolicy);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
    executor.setThreadFactory(new CustomThreadFactory(threadNamePrefix));
    executor.initialize();

    return executor;
  }


  private static class CustomThreadFactory implements java.util.concurrent.ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public CustomThreadFactory(String namePrefix) {
      this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());

      t.setDaemon(false);
      t.setPriority(Thread.NORM_PRIORITY);

      t.setUncaughtExceptionHandler((thread, throwable) -> {
        log.error("Uncaught exception in thread {}: {}", thread.getName(),
          throwable.getMessage(), throwable);
      });

      return t;
    }
  }
}
