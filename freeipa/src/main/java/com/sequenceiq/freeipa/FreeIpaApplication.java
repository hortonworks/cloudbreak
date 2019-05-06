package com.sequenceiq.freeipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableJpaRepositories(basePackages = { "com.sequenceiq.cloudbreak.repository", "com.sequenceiq.freeipa.repository" })
@SpringBootApplication(scanBasePackages = {"com.sequenceiq.freeipa", "com.sequenceiq.cloudbreak"})
public class FreeIpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreeIpaApplication.class, args);
    }

}

