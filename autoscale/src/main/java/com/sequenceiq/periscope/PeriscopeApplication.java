package com.sequenceiq.periscope;

import static com.sequenceiq.periscope.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableAutoConfiguration(exclude = WebMvcMetricsAutoConfiguration.class)
@EnableSwagger2
@ComponentScan(basePackages = {"com.sequenceiq"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
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
