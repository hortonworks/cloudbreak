package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoveryFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoveryRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SetupRecoverySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.cloudbreak.service.recovery.RdsRecoverySetupService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class RecoverySetupHandlerTest {

    private static final String EXCEPTION_MESSAGE = "Salt error";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private RecoverySetupHandler underTest;

    @Mock
    private RdsRecoverySetupService rdsRecoverySetupService;

    @Test
    void testDoAcceptWhenRecoveryThenSuccess() throws CloudbreakOrchestratorFailedException {

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent(ProvisionType.RECOVERY));

        assertEquals(EventSelectorUtil.selector(SetupRecoverySuccess.class), nextFlowStepSelector.selector());
        verify(rdsRecoverySetupService).addRecoverRole(STACK_ID);
    }

    @Test
    void testDoAcceptWhenRegularThenSuccess() throws CloudbreakOrchestratorFailedException {

        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent(ProvisionType.REGULAR));

        assertEquals(EventSelectorUtil.selector(SetupRecoverySuccess.class), nextFlowStepSelector.selector());
        verify(rdsRecoverySetupService, times(0)).addRecoverRole(STACK_ID);

    }

    @Test
    void testDoAcceptWhenExceptionThenFailure() throws CloudbreakOrchestratorFailedException {

        doThrow(new CloudbreakOrchestratorFailedException(EXCEPTION_MESSAGE)).when(rdsRecoverySetupService).addRecoverRole(STACK_ID);
        Selectable nextFlowStepSelector = underTest.doAccept(getHandlerEvent(ProvisionType.RECOVERY));

        assertEquals(EventSelectorUtil.selector(SetupRecoveryFailed.class), nextFlowStepSelector.selector());
        SetupRecoveryFailed failureEvent = (SetupRecoveryFailed) nextFlowStepSelector;
        assertEquals(EXCEPTION_MESSAGE, failureEvent.getException().getMessage());
    }

    private HandlerEvent<SetupRecoveryRequest> getHandlerEvent(ProvisionType provisionType) {
        SetupRecoveryRequest setupRecoveryRequest =
                new SetupRecoveryRequest(STACK_ID, provisionType);
        HandlerEvent<SetupRecoveryRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getData()).thenReturn(setupRecoveryRequest);
        return handlerEvent;
    }

}