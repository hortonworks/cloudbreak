package com.sequenceiq.redbeams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(scanBasePackages = {"com.sequenceiq.redbeams",
        "com.sequenceiq.cloudbreak.auth.altus",
        "com.sequenceiq.cloudbreak.auth.security",
        "com.sequenceiq.cloudbreak.security",
        "com.sequenceiq.cloudbreak.api.util",
        "com.sequenceiq.cloudbreak.conf",
        "com.sequenceiq.cloudbreak.config",
        "com.sequenceiq.cloudbreak.cache.common",
        "com.sequenceiq.cloudbreak.service.secret",
        "com.sequenceiq.cloudbreak.common.service",
        "com.sequenceiq.cloudbreak.common.dbmigration",
        "com.sequenceiq.cloudbreak.validation",
        "com.sequenceiq.cloudbreak.common.converter"},
        exclude = WebMvcMetricsAutoConfiguration.class)
public class RedbeamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedbeamsApplication.class, args);
    }

}
