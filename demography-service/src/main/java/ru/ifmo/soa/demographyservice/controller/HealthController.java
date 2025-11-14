package ru.ifmo.soa.demographyservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

  @GetMapping(produces="application/json")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("{\"status\":\"UP\"}");
  }
}
