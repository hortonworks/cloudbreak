package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowConfig {

    @Bean
    public Map<Class, FlowHandler> flowHandlersMap() {
        return new HashMap<>();
    }
}
