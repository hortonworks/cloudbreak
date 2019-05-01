package com.sequenceiq.usersync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(scanBasePackages = "com.sequenceiq.usersync", exclude = WebMvcMetricsAutoConfiguration.class)
public class UsersyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersyncApplication.class, args);
    }

}

