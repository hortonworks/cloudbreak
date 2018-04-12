package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import reactor.Environment;
import reactor.bus.EventBus;


@Configuration
@ComponentScan
public class TestConfig {

    @Bean
    public static PropertyResourceConfigurer propertyResourceConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public EventBus reactor(Environment env) {
        return EventBus.create(env);
    }

}
