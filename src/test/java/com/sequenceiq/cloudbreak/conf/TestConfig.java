package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningFacade;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.SynchronousDispatcher;

@Configuration
@ComponentScan(basePackageClasses = { FlowManager.class, ProvisioningFacade.class })
public class TestConfig {

    @Bean
    public static PropertyResourceConfigurer propertyResourceConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public Environment env() {
        return new Environment();
    }

    @Bean
    public Reactor reactor(Environment env) {
        return Reactors.reactor()
                .env(env)
                .dispatcher(new SynchronousDispatcher())
                .get();
    }

}
