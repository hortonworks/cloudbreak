package com.sequenceiq.caas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MockCaasApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(MockCaasApplication.class);
        } else {
            SpringApplication.run(MockCaasApplication.class, args);
        }
    }
}