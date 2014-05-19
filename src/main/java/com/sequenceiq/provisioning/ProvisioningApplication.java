package com.sequenceiq.provisioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.provisioning")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ProvisioningApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProvisioningApplication.class, args);
    }

}