package com.sequenceiq.cloudbreak.core.flow2.chain.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory;

@Configuration
public class FlowChainConfig {
    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    @Bean
    public Map<String, FlowEventChainFactory<?>> flowChainConfigMap() {
        Map<String, FlowEventChainFactory<?>> flowChainConfigMap = new HashMap<>();
        for (FlowEventChainFactory<?> flowEventChainFactory : flowChainFactories) {
            String key = flowEventChainFactory.initEvent();
            if (flowChainConfigMap.get(key) != null) {
                throw new UnsupportedOperationException("Event already registered: " + key);
            }
            flowChainConfigMap.put(key, flowEventChainFactory);
        }
        return ImmutableMap.copyOf(flowChainConfigMap);
    }
}
