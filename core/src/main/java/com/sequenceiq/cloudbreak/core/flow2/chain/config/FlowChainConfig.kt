package com.sequenceiq.cloudbreak.core.flow2.chain.config

import java.util.HashMap

import javax.annotation.Resource

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowEventChainFactory

@Configuration
class FlowChainConfig {
    @Resource
    private val flowChainFactories: List<FlowEventChainFactory<Payload>>? = null

    @Bean
    fun flowChainConfigMap(): Map<String, FlowEventChainFactory<Payload>> {
        val flowChainConfigMap = HashMap<String, FlowEventChainFactory<Payload>>()
        for (flowEventChainFactory in flowChainFactories!!) {
            val key = flowEventChainFactory.initEvent()
            if (flowChainConfigMap[key] != null) {
                throw UnsupportedOperationException("Event already registered: " + key)
            }
            flowChainConfigMap.put(key, flowEventChainFactory)
        }
        return ImmutableMap.copyOf(flowChainConfigMap)
    }
}
