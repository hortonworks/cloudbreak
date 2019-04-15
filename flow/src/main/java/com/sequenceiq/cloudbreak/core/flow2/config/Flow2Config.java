package com.sequenceiq.cloudbreak.core.flow2.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;

@Configuration
public class Flow2Config {
    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Bean
    public Map<String, FlowConfiguration<?>> flowConfigurationMap() {
        Map<String, FlowConfiguration<?>> flowConfigMap = new HashMap<>();
        for (FlowConfiguration<?> flowConfig : flowConfigs) {
            for (FlowEvent event : flowConfig.getInitEvents()) {
                String key = event.event();
                if (flowConfigMap.get(key) != null) {
                    throw new UnsupportedOperationException("Event already registered: " + key);
                }
                flowConfigMap.put(key, flowConfig);
            }
        }
        return ImmutableMap.copyOf(flowConfigMap);
    }

    @Bean
    public List<String> failHandledEvents(List<RetryableFlowConfiguration<?>> retryableFlowConfigurations) {
        return retryableFlowConfigurations.stream()
                .map(RetryableFlowConfiguration::getFailHandledEvent)
                .map(FlowEvent::event)
                .collect(Collectors.toList());
    }
}
