package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class CloudbreakApplication {

    public static void main(String[] args) {
        if (!versionedApplication().showVersionInfo(args)) {
            if (args.length == 0) {
                SpringApplication.run(CloudbreakApplication.class);
            } else {
                SpringApplication.run(CloudbreakApplication.class, args);
            }
        }
    }

}