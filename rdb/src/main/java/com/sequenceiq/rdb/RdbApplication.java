package com.sequenceiq.rdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(scanBasePackages = "com.sequenceiq.rdb", exclude = WebMvcMetricsAutoConfiguration.class)
public class RdbApplication {

    public static void main(String[] args) {
        SpringApplication.run(RdbApplication.class, args);
    }

}

