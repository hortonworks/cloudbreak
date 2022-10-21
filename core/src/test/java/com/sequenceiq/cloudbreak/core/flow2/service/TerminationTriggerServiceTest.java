package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;

@ExtendWith(MockitoExtension.class)
class TerminationTriggerServiceTest {

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private ApplicationFlowInformation applicationFlowInformation;

    @InjectMocks
    private TerminationTriggerService underTest;

    @BeforeEach
    void init() {
        lenient().when(applicationFlowInformation.getTerminationFlow())
                .thenReturn(List.of(StackTerminationFlowConfig.class, ClusterTerminationFlowConfig.class));
    }

    @AfterEach
    void validateCancelOldterminationFlowsCalled() {
        verify(flowCancelService).cancelTooOldTerminationFlowForResource(anyLong(), anyString());
    }

    @Test
    void whenStackNotDeletedAndNoFlowLogAndKerbAndNotForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(true, false, false);
    }

    @Test
    void whenStackNotDeletedAndNoFlowLogAndNotKerbAndNotForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(false, false, false);
    }

    @Test
    void whenStackNotDeletedAndNoFlowLogAndKerbAndForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true, false);
    }

    @Test
    void whenStackNotDeletedAndNoFlowLogAndNotKerbAndForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(false, true, false);
    }

    @Test
    void whenStackNotDeletedAndNotTerminationFlowLogAndKerbAndNotForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(StackCreationFlowConfig.class));
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(true, false, false);
    }

    @Test
    void whenStackNotDeletedAndNotTerminationFlowLogAndKerbAndForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(StackCreationFlowConfig.class));
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true, false);
    }

    @Test
    void whenStackNotDeletedAndNotTerminationFlowLogAndNotKerbAndNotForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(StackCreationFlowConfig.class));
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(false, false, false);
    }

    @Test
    void whenStackNotDeletedAndNotTerminationFlowLogAndNotKerbAndForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(StackCreationFlowConfig.class));
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(false, true, false);
    }

    @Test
    void whenStackNotDeletedAndNotForcedTerminationFlowLogAndNotForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(false)));

        underTest.triggerTermination(getAvailableStack(), false);

        verifyNoTerminationEventFired();
    }

    @Test
    void whenStackNotDeletedAndForcedTerminationFlowLogAndNotForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(true)));

        underTest.triggerTermination(getAvailableStack(), false);

        verifyNoTerminationEventFired();
    }

    @Test
    void whenStackNotDeletedAndForcedTerminationFlowLogAndForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(true)));

        underTest.triggerTermination(getAvailableStack(), true);

        verifyNoTerminationEventFired();
    }

    @Test
    void whenStackNotDeletedAndNotForcedTerminationFlowLogAndForcedShouldTerminate() {
        FlowLog flowLog = getTerminationFlowLog(false);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true, false);
        verify(flowCancelService).cancelFlowSilently(flowLog);
    }

    @Test
    void whenStackDeletedShouldNotTerminate() {
        Stack stack = stackWithStatus(Status.DELETE_COMPLETED);
        stack.setTerminated(2L);

        underTest.triggerTermination(stack, true);

        verifyNoTerminationEventFired();
    }

    @Test
    void whenStackDeletedButTerminationDateDidNotSetThenItShouldTerminate() {
        Stack stack = stackWithStatus(Status.DELETE_COMPLETED);

        underTest.triggerTermination(stack, true);

        verifyTerminationEventFired(false, true, false);
    }

    @Test
    void whenStackStoppedAndSecureThenItShouldTerminate() {
        Stack stack = stackWithStatus(Status.STOP_REQUESTED);

        underTest.triggerTermination(stack, false);

        verifyTerminationEventFired(true, false, true);
    }

    @Test
    void whenStackStoppedAndNotSecureThenItShouldTerminate() {
        Stack stack = stackWithStatus(Status.STOP_REQUESTED);

        underTest.triggerTermination(stack, false);

        verifyTerminationEventFired(false, false, true);
    }

    private Stack stackWithStatus(Status status) {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setEnvironmentCrn("envcrn");
        stack.setName("stackname");
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
        return stack;
    }

    private Stack getAvailableStack() {
        return stackWithStatus(Status.AVAILABLE);
    }

    private void verifyTerminationEventFired(boolean kerberized, boolean forced, boolean stopped) {
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TerminationEvent> eventCaptor = ArgumentCaptor.forClass(TerminationEvent.class);
        verify(reactorNotifier).notify(anyLong(), selectorCaptor.capture(), eventCaptor.capture());
        verify(flowCancelService).cancelRunningFlows(anyLong());

        String selector = selectorCaptor.getValue();
        TerminationEvent event = eventCaptor.getValue();
        if (kerberized && !stopped) {
            assertEquals(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT, selector);
            assertEquals(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT, event.selector());
        } else {
            assertEquals(FlowChainTriggers.TERMINATION_TRIGGER_EVENT, selector);
            assertEquals(FlowChainTriggers.TERMINATION_TRIGGER_EVENT, event.selector());
        }
        assertEquals(forced, event.getTerminationType().isForced());
    }

    private void verifyNoTerminationEventFired() {
        verify(reactorNotifier, never()).notify(anyLong(), anyString(), any(Acceptable.class));
        verify(flowCancelService, never()).cancelRunningFlows(anyLong());
    }

    private void setupKerberized() {
        when(kerberosConfigService.isKerberosConfigExistsForEnvironment(anyString(), anyString())).thenReturn(Boolean.TRUE);
    }

    private void setupNotKerberized() {
        when(kerberosConfigService.isKerberosConfigExistsForEnvironment(anyString(), anyString())).thenReturn(Boolean.FALSE);
    }

    private FlowLog getTerminationFlowLog(boolean forced) {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(StackTerminationFlowConfig.class));
        flowLog.setCurrentState("INIT_STATE");
        TerminationEvent event = new TerminationEvent("selector", 1L, forced ? TerminationType.FORCED : TerminationType.REGULAR);
        flowLog.setPayloadJackson(JsonUtil.writeValueAsStringSilent(event));
        flowLog.setPayloadType(ClassValue.of(TerminationEvent.class));
        return flowLog;
    }
}
