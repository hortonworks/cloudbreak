package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import com.sequenceiq.cloudbreak.core.init.CloudbreakCleanupAction;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableAutoConfiguration
@EnableSwagger2
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class CloudbreakApplication {

    public static void main(String[] args) {
        if (!versionedApplication().showVersionInfo(args)) {
            ConfigurableApplicationContext context;
            if (args.length == 0) {
                context = SpringApplication.run(CloudbreakApplication.class);
            } else {
                context = SpringApplication.run(CloudbreakApplication.class, args);
            }
            resetStackAndClusterStates(context);
        }
    }

    private static void resetStackAndClusterStates(ConfigurableApplicationContext context) {
        context.getBean(CloudbreakCleanupAction.class).resetStates();
    }

}