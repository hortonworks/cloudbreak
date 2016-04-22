package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;

public class Flow2ConfigTest {

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
        flowConfigs.add(new StackSyncFlowConfig());
        flowConfigs.add(new StackTerminationFlowConfig());
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        Map<String, FlowConfiguration<?>> flowConfigMap = underTest.flowConfigurationMap();
        assertEquals("Not all event type appeared in map!", 11, flowConfigMap.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFlowConfigurationMapInitIfAlreadyExists() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        StackSyncFlowConfig stackSyncFlowConfig = new StackSyncFlowConfig();
        flowConfigs.add(stackSyncFlowConfig);
        flowConfigs.add(stackSyncFlowConfig);
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        underTest.flowConfigurationMap();
    }
}
