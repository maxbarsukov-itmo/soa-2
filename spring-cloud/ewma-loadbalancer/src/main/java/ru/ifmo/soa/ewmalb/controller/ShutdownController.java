package ru.ifmo.soa.ewmalb.controller;

import org.springframework.web.bind.annotation.*;
import ru.ifmo.soa.ewmalb.lifecycle.GracefulShutdownManager;

import java.util.Map;

@RestController
@RequestMapping("/management")
public class ShutdownController {

  private final GracefulShutdownManager shutdownManager;

  public ShutdownController(GracefulShutdownManager shutdownManager) {
    this.shutdownManager = shutdownManager;
  }

  @PostMapping("/shutdown")
  public Map<String, Object> initiateShutdown() {
    if (shutdownManager.isShuttingDown()) {
      return Map.of(
        "status", "already_shutting_down",
        "message", "Shutdown already in progress",
        "timestamp", System.currentTimeMillis()
      );
    }

    new Thread(() -> {
      try {
        Thread.sleep(1000);
        shutdownManager.initiateGracefulShutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "shutdown-initiator").start();

    return Map.of(
      "status", "shutdown_initiated",
      "message", "Graceful shutdown initiated",
      "timestamp", System.currentTimeMillis()
    );
  }

  @PostMapping("/drain")
  public Map<String, Object> enableDrainMode() {
    boolean success = shutdownManager.enableDrainMode();

    return Map.of(
      "status", success ? "drain_enabled" : "cannot_enable_drain",
      "drain_mode", success,
      "shutting_down", shutdownManager.isShuttingDown(),
      "timestamp", System.currentTimeMillis()
    );
  }

  @DeleteMapping("/drain")
  public Map<String, Object> disableDrainMode() {
    boolean success = shutdownManager.disableDrainMode();

    return Map.of(
      "status", success ? "drain_disabled" : "cannot_disable_drain",
      "drain_mode", !success,
      "shutting_down", shutdownManager.isShuttingDown(),
      "timestamp", System.currentTimeMillis()
    );
  }

  @GetMapping("/status")
  public Map<String, Object> getShutdownStatus() {
    return Map.of(
      "shutting_down", shutdownManager.isShuttingDown(),
      "drain_mode", shutdownManager.isInDrainMode(),
      "timestamp", System.currentTimeMillis()
    );
  }

  @GetMapping("/health/detailed")
  public Map<String, Object> getDetailedHealth() {
    return Map.of(
      "status", shutdownManager.isShuttingDown() ? "SHUTTING_DOWN" : "UP",
      "drain_mode", shutdownManager.isInDrainMode(),
      "components", Map.of(
        "load_balancer", Map.of(
          "status", shutdownManager.isShuttingDown() ? "DRAINING" : "OPERATIONAL",
          "drain_mode", shutdownManager.isInDrainMode()
        )
      ),
      "timestamp", System.currentTimeMillis()
    );
  }
}
