package com.sequenceiq.redbeams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sequenceiq")
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = WebMvcMetricsAutoConfiguration.class)
public class RedbeamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedbeamsApplication.class, args);
    }

}
