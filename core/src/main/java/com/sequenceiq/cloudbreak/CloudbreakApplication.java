package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableAutoConfiguration
@EnableSwagger2
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
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