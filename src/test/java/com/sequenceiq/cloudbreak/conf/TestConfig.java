package com.sequenceiq.cloudbreak.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.core.flow.ProvisioningFacade;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.repository.SnsTopicRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.SynchronousDispatcher;

@Configuration
@ComponentScan(basePackageClasses = { FlowManager.class, SnsTopicRepository.class, ProvisioningFacade.class, PollingService.class })
public class TestConfig {

    @Autowired
    private List<ProvisionSetup> provisionSetups;

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

    @Bean
    public Map<CloudPlatform, ProvisionSetup> provisionSetups() {
        Map<CloudPlatform, ProvisionSetup> map = new HashMap<>();
        for (ProvisionSetup provisionSetup : provisionSetups) {
            map.put(provisionSetup.getCloudPlatform(), provisionSetup);
        }
        return map;
    }

}
