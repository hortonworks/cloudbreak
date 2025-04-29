package com.sequenceiq.flow.core.edh;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCloudbreakFlowEvent;
import static com.sequenceiq.flow.core.config.AbstractFlowConfiguration.FlowEdgeConfig;
import static com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowEvent.TEST_FAIL_HANDLED_EVENT;
import static com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState.FINAL_STATE;
import static com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState.INIT_STATE;
import static com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState.TEST_FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.core.config.TestFlowConfig.TestFlowState;
import com.sequenceiq.flow.core.listener.FlowTransitionContext;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class FlowUsageSenderTest {

    @InjectMocks
    private FlowUsageSender underTest;

    @Mock
    private ResourceIdProvider resourceIdProvider;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private UsageReporter usageReporter;

    @Test
    void testSendWhenNextFlowStateIsNull() {
        underTest.send(flowTransitionContext("rootFlowChainType/actualFlowChainType"), null, "flowEvent");
        verifyNoInteractions(resourceIdProvider, usageReporter, flowLogDBService);
    }

    @Test
    void testSendWhenResourceCrnNotFoundByResourceId() {
        when(resourceIdProvider.getResourceCrnByResourceId(eq(1L))).thenThrow(NotFoundException.class);
        underTest.send(flowTransitionContext("rootFlowChainType/actualFlowChainType"), INIT_STATE.name(), "flowEvent");
        verify(resourceIdProvider).getResourceCrnByResourceId(eq(1L));
        verifyNoInteractions(usageReporter, flowLogDBService);
    }

    @Test
    void testSendWhenResourceCrnIsEmpty() {
        when(resourceIdProvider.getResourceCrnByResourceId(eq(1L))).thenReturn("");
        underTest.send(flowTransitionContext("rootFlowChainType/actualFlowChainType"), INIT_STATE.name(), "flowEvent");
        verify(resourceIdProvider).getResourceCrnByResourceId(eq(1L));
        verifyNoInteractions(usageReporter, flowLogDBService);
    }

    @Test
    void testSendForFlowChainWhenResourceCrnIsNotEmptyWithoutReason() {
        when(resourceIdProvider.getResourceCrnByResourceId(eq(1L))).thenReturn("resourceCrn");
        underTest.send(flowTransitionContext("rootFlowChainType/actualFlowChainType"), INIT_STATE.name(), "flowEvent");
        verify(resourceIdProvider).getResourceCrnByResourceId(eq(1L));
        verifyNoInteractions(flowLogDBService);
        ArgumentCaptor<CDPCloudbreakFlowEvent> captor = ArgumentCaptor.forClass(CDPCloudbreakFlowEvent.class);
        verify(usageReporter).cdpCloudbreakFlowEvent(captor.capture());
        CDPCloudbreakFlowEvent event = captor.getValue();
        assertEquals("resourceCrn", event.getResourceCrn());
        assertEquals("rootFlowChainType", event.getRootFlowChainType());
        assertEquals("actualFlowChainType", event.getActualFlowChainType());
        assertEquals("flowType", event.getFlowType());
        assertEquals("TestFlowState", event.getStateType());
        assertEquals(INIT_STATE.name(), event.getFlowState());
        assertEquals("flowEvent", event.getFlowEvent());
        assertEquals("flowId", event.getFlowId());
        assertEquals("flowChainId", event.getFlowChainId());
        assertEquals("", event.getReason());
    }

    @Test
    void testSendForFlowWhenResourceCrnIsNotEmptyWithoutReason() {
        when(resourceIdProvider.getResourceCrnByResourceId(eq(1L))).thenReturn("resourceCrn");
        underTest.send(flowTransitionContext(null), INIT_STATE.name(), "flowEvent");
        verify(resourceIdProvider).getResourceCrnByResourceId(eq(1L));
        verifyNoInteractions(flowLogDBService);
        ArgumentCaptor<CDPCloudbreakFlowEvent> captor = ArgumentCaptor.forClass(CDPCloudbreakFlowEvent.class);
        verify(usageReporter).cdpCloudbreakFlowEvent(captor.capture());
        CDPCloudbreakFlowEvent event = captor.getValue();
        assertEquals("resourceCrn", event.getResourceCrn());
        assertEquals("", event.getRootFlowChainType());
        assertEquals("", event.getActualFlowChainType());
        assertEquals("flowType", event.getFlowType());
        assertEquals("TestFlowState", event.getStateType());
        assertEquals(INIT_STATE.name(), event.getFlowState());
        assertEquals("flowEvent", event.getFlowEvent());
        assertEquals("flowId", event.getFlowId());
        assertEquals("flowChainId", event.getFlowChainId());
        assertEquals("", event.getReason());
    }

    @Test
    void testSendWhenResourceCrnIsNotEmptyAndFlowFailed() {
        when(resourceIdProvider.getResourceCrnByResourceId(eq(1L))).thenReturn("resourceCrn");
        FlowLog flowLog = new FlowLog();
        flowLog.setReason("reason");
        when(flowLogDBService.findFirstByFlowIdOrderByCreatedDesc(eq("flowId"))).thenReturn(Optional.of(flowLog));
        underTest.send(flowTransitionContext("rootFlowChainType/actualFlowChainType"), TEST_FAILED_STATE.name(), "flowEvent");
        verify(resourceIdProvider).getResourceCrnByResourceId(eq(1L));
        verify(flowLogDBService).findFirstByFlowIdOrderByCreatedDesc(eq("flowId"));
        ArgumentCaptor<CDPCloudbreakFlowEvent> captor = ArgumentCaptor.forClass(CDPCloudbreakFlowEvent.class);
        verify(usageReporter).cdpCloudbreakFlowEvent(captor.capture());
        CDPCloudbreakFlowEvent event = captor.getValue();
        assertEquals("resourceCrn", event.getResourceCrn());
        assertEquals("rootFlowChainType", event.getRootFlowChainType());
        assertEquals("actualFlowChainType", event.getActualFlowChainType());
        assertEquals("flowType", event.getFlowType());
        assertEquals("TestFlowState", event.getStateType());
        assertEquals(TEST_FAILED_STATE.name(), event.getFlowState());
        assertEquals("flowEvent", event.getFlowEvent());
        assertEquals("flowId", event.getFlowId());
        assertEquals("flowChainId", event.getFlowChainId());
        assertEquals("reason", event.getReason());
    }

    private FlowTransitionContext flowTransitionContext(String flowChainType) {
        return new FlowTransitionContext(new FlowEdgeConfig(INIT_STATE, FINAL_STATE, TEST_FAILED_STATE, TEST_FAIL_HANDLED_EVENT),
                flowChainType, "flowType", TestFlowState.class, 1L, "flowId", "flowChainId", 1000L);
    }

}