package com.sequenceiq.it;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.testng.TestNG;

@SpringBootApplication
public class IntegrationTestApp implements CommandLineRunner {
    @Value("${integrationtest.testsuite.threadpool.size:3}")
    private int suiteThreadPoolSize;

    @Autowired
    private TestNG testng;

    @Override
    public void run(String... args) {
        System.setProperty("spring.freemarker.checkTemplateLocation", "false");
        testng.setSuiteThreadPoolSize(suiteThreadPoolSize);
        testng.setTestSuites(Arrays.asList(args));
        testng.run();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(IntegrationTestApp.class, args);
    }
}
