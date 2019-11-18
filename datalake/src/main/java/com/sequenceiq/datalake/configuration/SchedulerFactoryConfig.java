package com.sequenceiq.datalake.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerFactoryConfig {

    @Value("${datalake.autosync.enabled:false}")
    private boolean autoSyncEnabled;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> bean.setAutoStartup(autoSyncEnabled);
    }

}
