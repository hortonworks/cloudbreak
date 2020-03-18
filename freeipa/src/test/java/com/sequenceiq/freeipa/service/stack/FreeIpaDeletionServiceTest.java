package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class FreeIpaDeletionServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

    @InjectMocks
    private FreeIpaDeletionService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ChildEnvironmentService childEnvironmentService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private FreeipaJobService freeipaJobService;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private ApplicationFlowInformation applicationFlowInformation;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        stack.setStackStatus(stackStatus);
    }

    @Test
    void delete() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID))).thenReturn(Collections.singletonList(stack));
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(STACK_ID)).thenReturn(List.of());

        underTest.delete(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackService, times(1)).findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID));
        ArgumentCaptor<TerminationEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(TerminationEvent.class);
        verify(flowManager, times(1)).notify(eq(TERMINATION_EVENT.event()), terminationEventArgumentCaptor.capture());
        verify(flowCancelService).cancelTooOldTerminationFlowForResource(stack.getId(), stack.getName());
        verify(freeipaJobService).unschedule(stack);

        assertAll(
                () -> assertEquals(TERMINATION_EVENT.event(), terminationEventArgumentCaptor.getValue().selector()),
                () -> assertEquals(STACK_ID, terminationEventArgumentCaptor.getValue().getResourceId()),
                () -> assertFalse(terminationEventArgumentCaptor.getValue().getForced())
        );
    }

    @Test
    public void testAlreadyDeleted() {
        stack.getStackStatus().setStatus(Status.DELETE_COMPLETED);
        when(stackService.findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID))).thenReturn(Collections.singletonList(stack));

        underTest.delete(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(flowManager, never()).notify(anyString(), any(Acceptable.class));
        verify(flowCancelService, never()).cancelRunningFlows(stack.getId());
    }

    @Test
    public void testTerminationFlowExists() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID))).thenReturn(Collections.singletonList(stack));
        when(applicationFlowInformation.getTerminationFlow()).thenReturn(List.of(StackTerminationFlowConfig.class));
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(StackTerminationFlowConfig.class);
        flowLog.setCurrentState(StackTerminationState.INIT_STATE.name());
        when(flowLogService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(stack.getId())).thenReturn(List.of(flowLog));

        underTest.delete(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(flowManager, never()).notify(anyString(), any(Acceptable.class));
        verify(flowCancelService, never()).cancelRunningFlows(stack.getId());
    }

    @Test
    void deleteInvalid() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID))).thenReturn(Collections.singletonList(stack));
        when(childEnvironmentService.findChildEnvironments(stack, ACCOUNT_ID)).thenReturn(Collections.singletonList(new ChildEnvironment()));

        assertThrows(BadRequestException.class, () -> underTest.delete(ENVIRONMENT_CRN, ACCOUNT_ID));
        verify(stackService, times(1)).findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID));
        verify(childEnvironmentService, times(1)).findChildEnvironments(stack, ACCOUNT_ID);
    }
}