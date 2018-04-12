package com.sequenceiq.cloudbreak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

@EnableAutoConfiguration
@EnableSwagger2
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
@EnableAspectJAutoProxy(proxyTargetClass = true)
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