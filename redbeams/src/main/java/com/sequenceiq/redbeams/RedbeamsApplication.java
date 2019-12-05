package com.sequenceiq.redbeams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.sequenceiq.redbeams", "com.sequenceiq.flow", "com.sequenceiq.cloudbreak.ha.repository"})
@SpringBootApplication(scanBasePackages = {"com.sequenceiq.redbeams",
        "com.sequenceiq.authorization",
        "com.sequenceiq.cloudbreak.altus",
        "com.sequenceiq.cloudbreak.auth",
        "com.sequenceiq.cloudbreak.auth.altus",
        "com.sequenceiq.cloudbreak.auth.filter",
        "com.sequenceiq.cloudbreak.auth.security",
        "com.sequenceiq.cloudbreak.security",
        "com.sequenceiq.cloudbreak.api.util",
        "com.sequenceiq.cloudbreak.cloud.aws",
        "com.sequenceiq.cloudbreak.cloud.azure",
        "com.sequenceiq.cloudbreak.cloud.configuration",
        "com.sequenceiq.cloudbreak.cloud.credential",
        "com.sequenceiq.cloudbreak.cloud.handler",
        "com.sequenceiq.cloudbreak.cloud.init",
        "com.sequenceiq.cloudbreak.cloud.mock",
        "com.sequenceiq.cloudbreak.cloud.notification",
        "com.sequenceiq.cloudbreak.cloud.scheduler",
        "com.sequenceiq.cloudbreak.cloud.task",
        "com.sequenceiq.cloudbreak.cloud.template",
        "com.sequenceiq.cloudbreak.cloud.transform",
        "com.sequenceiq.cloudbreak.conf",
        "com.sequenceiq.cloudbreak.config",
        "com.sequenceiq.cloudbreak.cache.common",
        "com.sequenceiq.cloudbreak.service.secret",
        "com.sequenceiq.cloudbreak.common.database",
        "com.sequenceiq.cloudbreak.common.mappable",
        "com.sequenceiq.cloudbreak.common.service",
        "com.sequenceiq.cloudbreak.common.dbmigration",
        "com.sequenceiq.cloudbreak.validation",
        "com.sequenceiq.cloudbreak.common.converter",
        "com.sequenceiq.cloudbreak.json",
        "com.sequenceiq.cloudbreak.logger",
        "com.sequenceiq.cloudbreak.util",
        "com.sequenceiq.flow",
        "com.sequenceiq.environment.client",
        "com.sequenceiq.cloudbreak.client",
        "com.sequenceiq.cloudbreak.service",
        "com.sequenceiq.cloudbreak.ha.service",
        "com.sequenceiq.cloudbreak.tracing",
        "com.sequenceiq.cloudbreak.filter"},
        exclude = WebMvcMetricsAutoConfiguration.class)
public class RedbeamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedbeamsApplication.class, args);
    }

}
