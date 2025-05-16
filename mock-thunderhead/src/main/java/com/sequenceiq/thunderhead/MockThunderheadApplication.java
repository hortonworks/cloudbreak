package com.sequenceiq.thunderhead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication(scanBasePackages = {
        "com.sequenceiq.cloudbreak.auth.crn",
        "com.sequenceiq.cloudbreak.service.secret",
        "com.sequenceiq.cloudbreak.app",
        "com.sequenceiq.thunderhead",
        "com.sequenceiq.cloudbreak.dns",
        "com.sequenceiq.cloudbreak.clusterproxy"
})
public class MockThunderheadApplication {
    public static void main(String[] args) {
        if (args.length == 0) {
            SpringApplication.run(MockThunderheadApplication.class);
        } else {
            SpringApplication.run(MockThunderheadApplication.class, args);
        }
    }
}