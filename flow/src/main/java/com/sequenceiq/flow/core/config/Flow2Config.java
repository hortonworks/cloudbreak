package com.sequenceiq.flow.core.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.flow.core.FlowEvent;

@Configuration
public class Flow2Config {
    @Bean
    public Map<String, FlowConfiguration<?>> flowConfigurationMap(List<FlowConfiguration<?>> flowConfigs) {
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
    public Set<String> retryableEvents(List<RetryableFlowConfiguration<?>> retryableFlowConfigurations) {
        return retryableFlowConfigurations.stream()
                .map(RetryableFlowConfiguration::getRetryableEvent)
                .map(FlowEvent::event)
                .collect(Collectors.toSet());
    }

    @Bean
    public Set<String> failHandledEvents(List<AbstractFlowConfiguration<?, ?>> flowConfigurations) {
        return flowConfigurations.stream()
                .map(AbstractFlowConfiguration::getFailHandledEvent)
                .map(FlowEvent::event)
                .collect(Collectors.toSet());
    }
}
