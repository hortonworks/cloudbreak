package com.sequenceiq.cloudbreak.structuredevent.service.metric;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.TestEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.TestFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.TestFlowState;
import com.sequenceiq.flow.core.FlowMetricType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@ExtendWith(MockitoExtension.class)
class MetricStructuredEventSenderTest {

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Mock
    private MetricService metricService;

    @InjectMocks
    private MetricStructuredEventSender underTest;

    @BeforeEach
    void setUp() {
        flowConfigurations.add(new TestFlowConfig(TestFlowState.class, TestEvent.class));
        underTest.init();
    }

    @Test
    void testEventIsNotStructuredFlowEvent() {
        underTest.create(new CDPStructuredNotificationEvent());
        verifyNoInteractions(metricService);
    }

    @Test
    void testRequiredFlowParametersAreNull() {
        underTest.create(structuredFlowEvent(null, null, null));
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownFlowType() {
        underTest.create(structuredFlowEvent(null, "unknown", "INIT_STATE"));
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownNextFlowStateEnum() {
        underTest.create(structuredFlowEvent(null, "TestFlowConfig", "UNKNOWN_STATE"));
        verifyNoInteractions(metricService);
    }

    @Test
    void testNextFlowStateIsInit() {
        underTest.create(structuredFlowEvent("RootFlowChain/ActualFlowChain", "TestFlowConfig", "INIT_STATE"));
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_STARTED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFinal() {
        underTest.create(structuredFlowEvent("RootFlowChain/ActualFlowChain", "TestFlowConfig", "FINAL_STATE"));
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FINISHED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFailed() {
        underTest.create(structuredFlowEvent("RootFlowChain/ActualFlowChain", "TestFlowConfig", "FAILED_STATE"));
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FAILED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsOther() {
        underTest.create(structuredFlowEvent("RootFlowChain/ActualFlowChain", "TestFlowConfig", "TEMP_STATE"));
        verifyNoInteractions(metricService);
    }

    private static CDPStructuredFlowEvent structuredFlowEvent(String flowChainType, String flowType, String nextFlowState) {
        return new CDPStructuredFlowEvent(null, new FlowDetails(flowChainType, flowType, null, null, null, nextFlowState, null, 0L), null, null, null);
    }

}