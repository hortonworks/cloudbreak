package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableAsync
@EnableSwagger2
@ComponentScan(basePackages = "com.sequenceiq")
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.sequenceiq", repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableAutoConfiguration(exclude = WebMvcMetricsAutoConfiguration.class)
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