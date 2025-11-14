package ru.ifmo.soa.demographyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DemographyApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemographyApplication.class, args);
  }
}
