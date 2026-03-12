package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;

@ExtendWith(MockitoExtension.class)
class AbstractStackActionTest {

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private Stack stack;

    private TestAction underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestAction();
        ReflectionTestUtils.setField(underTest, "eventService", eventSenderService);
    }

    @Test
    void sendsNotificationWhenOperationTypeMapped() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);

        underTest.callSendFailedOperationNotificationIfApplicable(stack, "user", operation, "boom");

        verify(eventSenderService).sendEventAndNotification(eq(stack), eq("user"), eq(FREEIPA_UPGRADE_FAILED), eq(List.of("boom")));
    }

    @Test
    void doesNotSendNotificationWhenOperationTypeNotMapped() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.REPAIR);

        underTest.callSendFailedOperationNotificationIfApplicable(stack, "user", operation, "boom");

        verify(eventSenderService, never()).sendEventAndNotification(any(), any(), any(), any());
    }

    private static class TestAction extends AbstractStackAction<DummyState, DummyEvent, CommonContext, DummyPayload> {

        TestAction() {
            super(DummyPayload.class);
        }

        void callSendFailedOperationNotificationIfApplicable(Stack stack, String flowTriggerUserCrn, Operation operation, String errorReason) {
            sendFailedOperationNotificationIfApplicable(stack, flowTriggerUserCrn, operation, errorReason);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<DummyState, DummyEvent> stateContext, DummyPayload payload) {
            return null;
        }

        @Override
        protected void doExecute(CommonContext context, DummyPayload payload, java.util.Map<Object, Object> variables) {
            // not used in this unit test
        }

        @Override
        protected Object getFailurePayload(DummyPayload payload, Optional<CommonContext> flowContext, Exception ex) {
            return null;
        }
    }

    private enum DummyState implements FlowState {
        DUMMY;

        @Override
        public Class<? extends com.sequenceiq.flow.core.RestartAction> restartAction() {
            return null;
        }
    }

    private enum DummyEvent implements FlowEvent {
        DUMMY;

        @Override
        public String event() {
            return name();
        }
    }

    private static class DummyPayload implements Payload {
        @Override
        public Long getResourceId() {
            return 1L;
        }
    }
}

