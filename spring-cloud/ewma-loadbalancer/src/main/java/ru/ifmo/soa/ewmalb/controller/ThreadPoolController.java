package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.service.ThreadPoolMonitorService;

import java.util.Map;

@RestController
@RequestMapping("/management/thread-pools")
public class ThreadPoolController {

  private final ThreadPoolMonitorService threadPoolMonitor;

  public ThreadPoolController(ThreadPoolMonitorService threadPoolMonitor) {
    this.threadPoolMonitor = threadPoolMonitor;
  }

  @GetMapping("/stats")
  public Map<String, Object> getThreadPoolStats() {
    return Map.of(
      "timestamp", System.currentTimeMillis(),
      "loadBalancerPool", threadPoolMonitor.getLoadBalancerPoolStats(),
      "healthCheckPool", threadPoolMonitor.getHealthCheckPoolStats()
    );
  }

  @PostMapping("/scale-up")
  public Map<String, Object> scaleUp() {
    threadPoolMonitor.dynamicScaleUp();

    return Map.of(
      "action", "scale_up",
      "timestamp", System.currentTimeMillis(),
      "message", "Thread pool scaled up successfully"
    );
  }

  @PostMapping("/scale-down")
  public Map<String, Object> scaleDown() {
    threadPoolMonitor.dynamicScaleDown();

    return Map.of(
      "action", "scale_down",
      "timestamp", System.currentTimeMillis(),
      "message", "Thread pool scaled down successfully"
    );
  }
}
