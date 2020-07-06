package com.sequenceiq.thunderhead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MockThunderheadApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(MockThunderheadApplication.class);
        } else {
            SpringApplication.run(MockThunderheadApplication.class, args);
        }
    }
}