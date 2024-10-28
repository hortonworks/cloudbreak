package com.sequenceiq.flow.core.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.flow.core.FlowMetricType;
import com.sequenceiq.flow.core.config.TestFlowConfig;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState;

@ExtendWith(MockitoExtension.class)
class FlowMetricSenderTest {

    @Mock
    private MetricService metricService;

    @InjectMocks
    private FlowMetricSender underTest;

    @Test
    void testRequiredFlowParametersAreNull() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", null, TestFlowState.class, System.currentTimeMillis(), null, null);
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownNextFlowStateEnum() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", null, TestFlowState.class, System.currentTimeMillis(), "UNKNOWN_STATE", null);
        verifyNoInteractions(metricService);
    }

    @Test
    void testNextFlowStateIsInit() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", "RootFlowChain/ActualFlowChain", TestFlowState.class,
                System.currentTimeMillis(), "INIT_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_STARTED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFinal() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", "RootFlowChain/ActualFlowChain", TestFlowState.class,
                System.currentTimeMillis(), "FINAL_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FINISHED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFailed() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", "RootFlowChain/ActualFlowChain", TestFlowState.class,
                System.currentTimeMillis(), "TEST_FAILED_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FAILED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsOther() {
        underTest.send(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig", "RootFlowChain/ActualFlowChain", TestFlowState.class,
                System.currentTimeMillis(), "TEMP_STATE", null);
        verifyNoInteractions(metricService);
    }
}