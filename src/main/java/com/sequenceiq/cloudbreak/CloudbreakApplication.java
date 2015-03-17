package com.sequenceiq.cloudbreak;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class CloudbreakApplication extends VersionedApplication {

    public static void main(String[] args) {
        start(CloudbreakApplication.class, args);
    }

}