package com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.handler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordType;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordHandlerTest {

    private static final long STACK_ID = 1L;

    private static final HandlerEvent<RotateSaltPasswordRequest> EVENT = new HandlerEvent<>(new Event<>(
            new RotateSaltPasswordRequest(STACK_ID, RotateSaltPasswordReason.MANUAL, RotateSaltPasswordType.SALT_BOOTSTRAP_ENDPOINT)));

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Mock
    private Stack stack;

    @InjectMocks
    private RotateSaltPasswordHandler underTest;

    @BeforeEach
    void setUp() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    @Test
    void testSuccess() {
        Selectable result = underTest.doAccept(EVENT);

        Assertions.assertThat(result)
                .isInstanceOf(RotateSaltPasswordSuccessResponse.class)
                .returns(STACK_ID, Payload::getResourceId);
        verify(rotateSaltPasswordService).rotateSaltPassword(stack);
    }

    @Test
    void testSuccessFallback() {
        HandlerEvent<RotateSaltPasswordRequest> event = new HandlerEvent<>(new Event<>(
                new RotateSaltPasswordRequest(STACK_ID, RotateSaltPasswordReason.MANUAL, RotateSaltPasswordType.FALLBACK)));

        Selectable result = underTest.doAccept(event);

        Assertions.assertThat(result)
                .isInstanceOf(RotateSaltPasswordSuccessResponse.class)
                .returns(STACK_ID, Payload::getResourceId);
        verify(rotateSaltPasswordService).rotateSaltPassword(stack);
    }

    @Test
    void testFailure() {
        CloudbreakServiceException exception = new CloudbreakServiceException("oops");
        doThrow(exception).when(rotateSaltPasswordService).rotateSaltPassword(stack);

        Selectable result = underTest.doAccept(EVENT);

        Assertions.assertThat(result)
                .isInstanceOf(RotateSaltPasswordFailureResponse.class)
                .extracting(RotateSaltPasswordFailureResponse.class::cast)
                .returns(STACK_ID, Payload::getResourceId)
                .returns(exception, StackFailureEvent::getException);
    }

}