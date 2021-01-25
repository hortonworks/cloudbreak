package com.sequenceiq.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = QuartzAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class MockInfrastructureApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(MockInfrastructureApplication.class);
        } else {
            SpringApplication.run(MockInfrastructureApplication.class, args);
        }
    }
}
