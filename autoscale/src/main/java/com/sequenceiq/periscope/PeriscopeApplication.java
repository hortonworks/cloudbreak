package com.sequenceiq.periscope;

import static com.sequenceiq.periscope.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.periscope")
public class PeriscopeApplication {

    public static void main(String[] args) {
        if (!versionedApplication().showVersionInfo(args)) {
            if (args.length == 0) {
                SpringApplication.run(PeriscopeApplication.class);
            } else {
                SpringApplication.run(PeriscopeApplication.class, args);
            }
        }
    }

}
