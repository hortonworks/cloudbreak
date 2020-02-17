package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
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

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
    }

    @Test
    void delete() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID))).thenReturn(Collections.singletonList(stack));

        underTest.delete(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackService, times(1)).findAllByEnvironmentCrnAndAccountId(eq(ENVIRONMENT_CRN), eq(ACCOUNT_ID));

        ArgumentCaptor<TerminationEvent> terminationEventArgumentCaptor = ArgumentCaptor.forClass(TerminationEvent.class);
        verify(flowManager, times(1)).notify(eq(TERMINATION_EVENT.event()), terminationEventArgumentCaptor.capture());

        assertAll(
                () -> assertEquals(TERMINATION_EVENT.event(), terminationEventArgumentCaptor.getValue().selector()),
                () -> assertEquals(STACK_ID, terminationEventArgumentCaptor.getValue().getResourceId()),
                () -> assertFalse(terminationEventArgumentCaptor.getValue().getForced())
        );
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