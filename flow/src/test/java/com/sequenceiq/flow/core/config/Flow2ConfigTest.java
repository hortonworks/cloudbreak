package com.sequenceiq.flow.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldFlowConfig;

public class Flow2ConfigTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private Flow2Config underTest;

    @Before
    public void setUp() {
        underTest = new Flow2Config();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFlowConfigurationMapInit() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        flowConfigs.add(new HelloWorldFlowConfig());
        flowConfigs.add(new TestFlowConfig());

        Map<String, FlowConfiguration<?>> flowConfigMap = underTest.flowConfigurationMap(flowConfigs);
        assertEquals("Not all flow type appeared in map!", countEvents(flowConfigs), flowConfigMap.size());
    }

    @Test
    public void testFlowConfigurationMapInitIfAlreadyExists() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        flowConfigs.add(helloWorldFlowConfig);
        flowConfigs.add(helloWorldFlowConfig);
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Event already registered: " + HelloWorldEvent.HELLOWORLD_TRIGGER_EVENT.event());
        underTest.flowConfigurationMap(flowConfigs);
    }

    @Test
    public void testFailHandledEventsEmptyCollection() {
        assertTrue(underTest.failHandledEvents(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testFailHandledEvents() {
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        TestFlowConfig testFlowConfig = new TestFlowConfig();

        List<RetryableFlowConfiguration<?>> retryableFlowConfigurations = Lists.newArrayList(helloWorldFlowConfig, testFlowConfig);
        List<String> failHandledEvents = underTest.failHandledEvents(retryableFlowConfigurations);

        List<String> expected = Lists.newArrayList(helloWorldFlowConfig.getFailHandledEvent().event(),
                testFlowConfig.getFailHandledEvent().event());
        assertEquals(expected, failHandledEvents);
    }

    private int countEvents(List<FlowConfiguration<?>> flowConfigs) {
        return flowConfigs.stream()
                .mapToInt(c -> c.getInitEvents().length)
                .sum();
    }
}
