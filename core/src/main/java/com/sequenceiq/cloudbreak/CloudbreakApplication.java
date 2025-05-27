package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import com.sequenceiq.cloudbreak.structuredevent.service.CDPFlowStructuredEventHandler;
import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;

@EnableAsync
@ComponentScan(basePackages = "com.sequenceiq",
        //TODO eliminate this exclude filter for CDPFlowStructuredEventHandler.class with https://cloudera.atlassian.net/browse/CB-18923
        excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CDPFlowStructuredEventHandler.class) })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.sequenceiq", repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableAutoConfiguration(exclude = { ErrorMvcAutoConfiguration.class, WebMvcObservationAutoConfiguration.class })
public class CloudbreakApplication {
    public static void main(String[] args) {
        BouncyCastleFipsProviderLoader.load();
        if (!versionedApplication().showVersionInfo(args)) {
            if (args.length == 0) {
                SpringApplication.run(CloudbreakApplication.class);
            } else {
                SpringApplication.run(CloudbreakApplication.class, args);
            }
        }
    }
}
