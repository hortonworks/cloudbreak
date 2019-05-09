package com.sequenceiq.datalake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableJpaRepositories(basePackages = { "com.sequenceiq.datalake", "com.sequenceiq.cloudbreak.common" })
@EnableSwagger2
@SpringBootApplication(scanBasePackages = { "com.sequenceiq.datalake", "com.sequenceiq.cloudbreak.common" }, exclude = WebMvcMetricsAutoConfiguration.class)
public class DatalakeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatalakeApplication.class, args);
    }

}

