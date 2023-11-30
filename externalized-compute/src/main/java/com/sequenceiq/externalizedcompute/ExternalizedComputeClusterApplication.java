package com.sequenceiq.externalizedcompute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = { "com.sequenceiq" })
@SpringBootApplication(scanBasePackages = { "com.sequenceiq" }, exclude = { ErrorMvcAutoConfiguration.class, WebMvcObservationAutoConfiguration.class })
public class ExternalizedComputeClusterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalizedComputeClusterApplication.class, args);
    }

}
