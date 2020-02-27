package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;

@RunWith(MockitoJUnitRunner.class)
public class TerminationTriggerServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationTriggerServiceTest.class);

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

    @Before
    public void init() {
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(List.of(StackTerminationFlowConfig.class, ClusterTerminationFlowConfig.class));
    }

    @After
    public void validateCancelOldterminationFlowsCalled() {
        verify(flowCancelService).cancelTooOldTerminationFlowForResource(anyLong(), anyString());
    }

    @Test
    public void whenStackNotDeletedAndNoFlowLogAndKerbAndNotForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(true, false);
    }

    @Test
    public void whenStackNotDeletedAndNoFlowLogAndNotKerbAndNotForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(false, false);
    }

    @Test
    public void whenStackNotDeletedAndNoFlowLogAndKerbAndForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true);
    }

    @Test
    public void whenStackNotDeletedAndNoFlowLogAndNotKerbAndForcedShouldTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of());
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(false, true);
    }

    @Test
    public void whenStackNotDeletedAndNotTerminationFlowLogAndKerbAndNotForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(StackCreationFlowConfig.class);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(true, false);
    }

    @Test
    public void whenStackNotDeletedAndNotTerminationFlowLogAndKerbAndForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(StackCreationFlowConfig.class);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true);
    }

    @Test
    public void whenStackNotDeletedAndNotTerminationFlowLogAndNotKerbAndNotForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(StackCreationFlowConfig.class);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), false);

        verifyTerminationEventFired(false, false);
    }

    @Test
    public void whenStackNotDeletedAndNotTerminationFlowLogAndNotKerbAndForcedShouldTerminate() {
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(StackCreationFlowConfig.class);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupNotKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(false, true);
    }

    @Test
    public void whenStackNotDeletedAndNotForcedTerminationFlowLogAndNotForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(false)));

        underTest.triggerTermination(getAvailableStack(), false);

        verifyNoTerminationEventFired();
    }

    @Test
    public void whenStackNotDeletedAndForcedTerminationFlowLogAndNotForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(true)));

        underTest.triggerTermination(getAvailableStack(), false);

        verifyNoTerminationEventFired();
    }

    @Test
    public void whenStackNotDeletedAndForcedTerminationFlowLogAndForcedShouldNotTerminate() {
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(getTerminationFlowLog(true)));

        underTest.triggerTermination(getAvailableStack(), true);

        verifyNoTerminationEventFired();
    }

    @Test
    public void whenStackNotDeletedAndNotForcedTerminationFlowLogAndForcedShouldTerminate() {
        FlowLog flowLog = getTerminationFlowLog(false);
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(anyLong())).thenReturn(List.of(flowLog));
        setupKerberized();

        underTest.triggerTermination(getAvailableStack(), true);

        verifyTerminationEventFired(true, true);
        verify(flowCancelService).cancelFlowSilently(flowLog);
    }

    @Test
    public void whenStackDeletedShouldNotTerminate() {
        Stack stack = stackWithStatus(Status.DELETE_COMPLETED);

        underTest.triggerTermination(stack, true);

        verifyNoTerminationEventFired();
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

    private void verifyTerminationEventFired(boolean kerberized, boolean forced) {
        ArgumentCaptor<String> selectorCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TerminationEvent> eventCaptor = ArgumentCaptor.forClass(TerminationEvent.class);
        verify(reactorNotifier).notify(anyLong(), selectorCaptor.capture(), eventCaptor.capture());
        verify(flowCancelService).cancelRunningFlows(anyLong());

        String selector = selectorCaptor.getValue();
        TerminationEvent event = eventCaptor.getValue();
        if (kerberized) {
            assertEquals(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT, selector);
            assertEquals(FlowChainTriggers.PROPER_TERMINATION_TRIGGER_EVENT, event.selector());
        } else {
            assertEquals(FlowChainTriggers.TERMINATION_TRIGGER_EVENT, selector);
            assertEquals(FlowChainTriggers.TERMINATION_TRIGGER_EVENT, event.selector());
        }
        assertEquals(forced, event.getForced());
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
        flowLog.setFlowType(StackTerminationFlowConfig.class);
        flowLog.setCurrentState("INIT_STATE");
        TerminationEvent event = new TerminationEvent("selector", 1L, forced);
        flowLog.setPayload(JsonWriter.objectToJson(event));
        flowLog.setPayloadType(TerminationEvent.class);
        return flowLog;
    }
}