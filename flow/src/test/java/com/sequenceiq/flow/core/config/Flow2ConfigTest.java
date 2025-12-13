package com.sequenceiq.flow.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;

class Flow2ConfigTest {

    private Flow2Config underTest = new Flow2Config();

    @Test
    void testFlowConfigurationMapInit() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        flowConfigs.add(new HelloWorldFlowConfig());
        flowConfigs.add(new TestFlowConfig());

        Map<String, FlowConfiguration<?>> flowConfigMap = underTest.flowConfigurationMap(flowConfigs);
        assertEquals(countEvents(flowConfigs), flowConfigMap.size(), "Not all flow type appeared in map!");
    }

    @Test
    void testFlowConfigurationMapInitIfAlreadyExists() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        flowConfigs.add(helloWorldFlowConfig);
        flowConfigs.add(helloWorldFlowConfig);
        assertThrows(UnsupportedOperationException.class, () -> underTest.flowConfigurationMap(flowConfigs),
                "Event already registered: " + HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT.event());
    }

    @Test
    void testEmptyretRyableEvents() {
        assertTrue(underTest.retryableEvents(Collections.emptyList()).isEmpty());
    }

    @Test
    void testRetryableEvents() {
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        TestFlowConfig testFlowConfig = new TestFlowConfig();

        List<RetryableFlowConfiguration<?>> retryableFlowConfigurations = List.of(helloWorldFlowConfig, testFlowConfig);
        Set<String> retryableEvents = underTest.retryableEvents(retryableFlowConfigurations);

        Set<String> expected = Set.of(helloWorldFlowConfig.getRetryableEvent().event(),
                testFlowConfig.getRetryableEvent().event());
        assertEquals(expected, retryableEvents);
    }

    private int countEvents(List<FlowConfiguration<?>> flowConfigs) {
        return flowConfigs.stream()
                .mapToInt(c -> c.getInitEvents().length)
                .sum();
    }
}
