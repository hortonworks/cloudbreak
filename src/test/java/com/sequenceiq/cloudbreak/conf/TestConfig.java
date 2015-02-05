package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.sequenceiq.cloudbreak.core.flow.FlowConfig;
import com.sequenceiq.cloudbreak.core.flow.FlowHandler;
import com.sequenceiq.cloudbreak.core.flow.FlowManager;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.SynchronousDispatcher;

@Configuration
@ComponentScan(basePackageClasses = { FlowManager.class, FlowHandler.class, ProvisionSetup.class })
@Import(FlowConfig.class)
public class TestConfig {

//    @Autowired
//    private List<ProvisionSetup> provisionSetups;
//
//    @Autowired
//    private List<FlowHandler> flowHandlers;

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

    //    @Bean
    //    public Map<CloudPlatform, ProvisionSetup> provisionSetups() {
    //        Map<CloudPlatform, ProvisionSetup> map = new HashMap<>();
    //        for (ProvisionSetup provisionSetup : provisionSetups) {
    //            map.put(provisionSetup.getCloudPlatform(), provisionSetup);
    //        }
    //        return map;
    //    }

}
