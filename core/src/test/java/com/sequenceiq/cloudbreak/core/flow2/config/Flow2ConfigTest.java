package com.sequenceiq.cloudbreak.core.flow2.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;

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
        flowConfigs.add(new StackSyncFlowConfig());
        flowConfigs.add(new StackTerminationFlowConfig());
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        Map<String, FlowConfiguration<?>> flowConfigMap = underTest.flowConfigurationMap();
        assertEquals("Not all flow type appeared in map!", countEvents(flowConfigs), flowConfigMap.size());
    }

    @Test
    public void testFlowConfigurationMapInitIfAlreadyExists() {
        List<FlowConfiguration<?>> flowConfigs = new ArrayList<>();
        StackSyncFlowConfig stackSyncFlowConfig = new StackSyncFlowConfig();
        flowConfigs.add(stackSyncFlowConfig);
        flowConfigs.add(stackSyncFlowConfig);
        given(this.flowConfigs.iterator()).willReturn(flowConfigs.iterator());
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("Event already registered: STACK_SYNC_TRIGGER_EVENT");
        underTest.flowConfigurationMap();
    }

    private int countEvents(List<FlowConfiguration<?>> flowConfigs) {
        return flowConfigs.stream()
                .mapToInt(c -> c.getInitEvents().length)
                .sum();
    }
}
