package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.handler;

import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationFailed;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationSuccess;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class VerifyReplicationConvergenceHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ChangePrimaryGatewayService changePrimaryGatewayService;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @InjectMocks
    private VerifyReplicationConvergenceHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(ChangePrimaryGatewayVerifyReplicationRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception exception = new RuntimeException("Test exception");
        Event<ChangePrimaryGatewayVerifyReplicationRequest> event = new Event<>(new ChangePrimaryGatewayVerifyReplicationRequest(STACK_ID));

        Selectable result = underTest.defaultFailureEvent(STACK_ID, exception, event);

        ChangePrimaryGatewayVerifyReplicationFailed failureEvent = assertInstanceOf(ChangePrimaryGatewayVerifyReplicationFailed.class, result);
        assertEquals(STACK_ID, failureEvent.getResourceId());
        assertEquals(ChangePrimaryGatewayVerifyReplicationFailed.FAILED_PHASE, failureEvent.getFailedPhase());
        assertEquals(Set.of(), failureEvent.getSuccess());
        assertEquals(Map.of(), failureEvent.getFailureDetails());
        assertSame(exception, failureEvent.getException());
        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FAILED_EVENT.event(), failureEvent.selector());
    }

    @Test
    void testDoAcceptSuccess() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        Selectable result = underTest.doAccept(handlerEvent());

        verify(changePrimaryGatewayService).verifyReplicationConvergenceOnPrimaryGateway(stack);
        ChangePrimaryGatewayVerifyReplicationSuccess success = assertInstanceOf(ChangePrimaryGatewayVerifyReplicationSuccess.class, result);
        assertEquals(STACK_ID, success.getResourceId());
        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT.event(), success.selector());
    }

    @Test
    void testDoAcceptReturnsFailedEventOnOrchestratorException() throws Exception {
        CloudbreakOrchestratorFailedException orchestratorException = new CloudbreakOrchestratorFailedException("Replication did not converge");
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(orchestratorException).when(changePrimaryGatewayService).verifyReplicationConvergenceOnPrimaryGateway(stack);

        Selectable result = underTest.doAccept(handlerEvent());

        ChangePrimaryGatewayVerifyReplicationFailed failed = assertInstanceOf(ChangePrimaryGatewayVerifyReplicationFailed.class, result);
        assertEquals(STACK_ID, failed.getResourceId());
        assertSame(orchestratorException, failed.getException());
        assertEquals(ChangePrimaryGatewayVerifyReplicationFailed.FAILED_PHASE, failed.getFailedPhase());
        assertEquals(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FAILED_EVENT.event(), failed.selector());
    }

    @Test
    void testDoAcceptPropagatesUnexpectedExceptionForTheCatcherToHandle() throws Exception {
        RuntimeException unexpected = new IllegalStateException("unexpected");
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(unexpected).when(changePrimaryGatewayService).verifyReplicationConvergenceOnPrimaryGateway(stack);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> underTest.doAccept(handlerEvent()));

        assertSame(unexpected, thrown);
    }

    private HandlerEvent<ChangePrimaryGatewayVerifyReplicationRequest> handlerEvent() {
        return new HandlerEvent<>(new Event<>(new ChangePrimaryGatewayVerifyReplicationRequest(STACK_ID)));
    }
}
