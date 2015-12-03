package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

@Configuration
public class Flow2Config {
    @Resource
    private List<FlowConfiguration<?, ?>> flowConfigs;

    @Bean
    public Map<String, FlowConfiguration<?, ?>> flowConfigurationMap() {
        Map<String, FlowConfiguration<?, ?>> flowConfigMap = new HashMap<>();
        for (FlowConfiguration<?, ?> flowConfig : flowConfigs) {
            for (FlowEvent event : flowConfig.getFlowTriggerEvents()) {
                flowConfigMap.put(event.stringRepresentation(), flowConfig);
            }
        }
        return Collections.unmodifiableMap(flowConfigMap);
    }
}
