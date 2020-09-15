package com.sequenceiq.cloudbreak.quartz.configuration;

import javax.inject.Inject;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@Configuration
public class SchedulerFactoryConfig {

    @Inject
    private StatusCheckerConfig properties;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> {
            bean.setAutoStartup(properties.isAutoSyncEnabled());
        };
    }

}
