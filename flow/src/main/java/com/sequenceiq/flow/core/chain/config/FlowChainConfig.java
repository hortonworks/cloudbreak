package com.sequenceiq.flow.core.chain.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Configuration
public class FlowChainConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainConfig.class);

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
            LOGGER.info("Registering init event: {} for flow: {} in flowChainConfigMap", key, flowEventChainFactory.getName());
            flowChainConfigMap.put(key, flowEventChainFactory);
        }
        return ImmutableMap.copyOf(flowChainConfigMap);
    }
}
