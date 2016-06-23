package com.sequenceiq.cloudbreak.core.flow2.config

import java.util.HashMap

import javax.annotation.Resource

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.google.common.collect.ImmutableMap
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent

@Configuration
class Flow2Config {
    @Resource
    private val flowConfigs: List<FlowConfiguration<*>>? = null

    @Bean
    fun flowConfigurationMap(): Map<String, FlowConfiguration<*>> {
        val flowConfigMap = HashMap<String, FlowConfiguration<*>>()
        for (flowConfig in flowConfigs!!) {
            for (event in flowConfig.initEvents) {
                val key = event.stringRepresentation()
                if (flowConfigMap[key] != null) {
                    throw UnsupportedOperationException("Event already registered: " + key)
                }
                flowConfigMap.put(key, flowConfig)
            }
        }
        return ImmutableMap.copyOf(flowConfigMap)
    }
}
