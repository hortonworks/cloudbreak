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
import com.sequenceiq.flow.core.listener.FlowTransitionContext;

@ExtendWith(MockitoExtension.class)
class FlowMetricSenderTest {

    @Mock
    private MetricService metricService;

    @InjectMocks
    private FlowMetricSender underTest;

    @Test
    void testRequiredFlowParametersAreNull() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                null, TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, null, null);
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownNextFlowStateEnum() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                null, TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, "UNKNOWN_STATE", null);
        verifyNoInteractions(metricService);
    }

    @Test
    void testNextFlowStateIsInit() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                "RootFlowChain/ActualFlowChain", TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, "INIT_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_STARTED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFinal() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                "RootFlowChain/ActualFlowChain", TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, "FINAL_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FINISHED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFailed() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                "RootFlowChain/ActualFlowChain", TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, "TEST_FAILED_STATE", null);
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FAILED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsOther() {
        FlowTransitionContext flowTransitionContext = new FlowTransitionContext(new TestFlowConfig().getEdgeConfig(), "TestFlowConfig",
                "RootFlowChain/ActualFlowChain", TestFlowState.class, null, null, null, System.currentTimeMillis());
        underTest.send(flowTransitionContext, "TEMP_STATE", null);
        verifyNoInteractions(metricService);
    }
}