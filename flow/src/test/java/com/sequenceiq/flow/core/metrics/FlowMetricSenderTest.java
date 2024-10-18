package com.sequenceiq.flow.core.metrics;

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
import com.sequenceiq.flow.core.FlowMetricType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.TestFlowConfig;

@ExtendWith(MockitoExtension.class)
class FlowMetricSenderTest {

    @Spy
    private List<AbstractFlowConfiguration> flowConfigurations = new ArrayList<>();

    @Mock
    private MetricService metricService;

    @InjectMocks
    private FlowMetricSender underTest;

    @BeforeEach
    void setUp() {
        flowConfigurations.add(new TestFlowConfig());
        underTest.init();
    }

    @Test
    void testRequiredFlowParametersAreNull() {
        underTest.send(null, null, null, null, System.currentTimeMillis());
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownFlowType() {
        underTest.send("unknown", null, "INIT_STATE", null, System.currentTimeMillis());
        verifyNoInteractions(metricService);
    }

    @Test
    void testUnknownNextFlowStateEnum() {
        underTest.send("TestFlowConfig", null, "UNKNOWN_STATE", null, System.currentTimeMillis());
        verifyNoInteractions(metricService);
    }

    @Test
    void testNextFlowStateIsInit() {
        underTest.send("TestFlowConfig", "RootFlowChain/ActualFlowChain", "INIT_STATE", null,
                System.currentTimeMillis());
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_STARTED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFinal() {
        underTest.send("TestFlowConfig", "RootFlowChain/ActualFlowChain", "FINAL_STATE", null,
                System.currentTimeMillis());
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FINISHED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsFailed() {
        underTest.send("TestFlowConfig", "RootFlowChain/ActualFlowChain", "TEST_FAILED_STATE", null,
                System.currentTimeMillis());
        verify(metricService, times(1)).incrementMetricCounter(eq(FlowMetricType.FLOW_FAILED), any(String[].class));
    }

    @Test
    void testNextFlowStateIsOther() {
        underTest.send("TestFlowConfig", "RootFlowChain/ActualFlowChain", "TEMP_STATE", null,
                System.currentTimeMillis());
        verifyNoInteractions(metricService);
    }
}