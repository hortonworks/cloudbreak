package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.core.flow2.helloworld.HelloWorldFlowConfig;

public class Flow2ConfigTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private Flow2Config underTest;

    @Mock
    private List<FlowConfiguration<?>> flowConfigs;

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
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        Map<String, FlowConfiguration<?>> flowConfigMap = underTest.flowConfigurationMap();
        assertEquals("Not all flow type appeared in map!", countEvents(flowConfigs), flowConfigMap.size());
    }

    @Test
    public void testFlowConfigurationMapInitIfAlreadyExists() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        HelloWorldFlowConfig helloWorldFlowConfig = new HelloWorldFlowConfig();
        flowConfigs.add(helloWorldFlowConfig);
        flowConfigs.add(helloWorldFlowConfig);
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Event already registered: START_HELLO_WORLD_EVENT");
        underTest.flowConfigurationMap();
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
