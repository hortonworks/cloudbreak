package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FAILED;
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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class AbstractStackActionTest {

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private Stack stack;

    private TestAction underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestAction();
        ReflectionTestUtils.setField(underTest, "eventService", eventSenderService);
        ReflectionTestUtils.setField(underTest, "stackUpdater", stackUpdater);
    }

    @Test
    void sendsNotificationWhenOperationTypeMapped() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);

        underTest.callSendFailedOperationNotificationIfApplicable(stack, "user", operation, "boom");

        verify(eventSenderService).sendEventAndNotification(eq(stack), eq("user"), eq(FREEIPA_UPGRADE_FAILED), eq(List.of("boom")));
    }

    @Test
    void sendsMultiAzMigrationNotificationWhenOperationTypeIsMigrateToMultiAz() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.MIGRATE_TO_MULTI_AZ);

        underTest.callSendFailedOperationNotificationIfApplicable(stack, "user", operation, "boom");

        verify(eventSenderService).sendEventAndNotification(eq(stack), eq("user"), eq(FREEIPA_MULTI_AZ_MIGRATION_FAILED), eq(List.of("boom")));
    }

    @Test
    void doesNotSendNotificationWhenOperationTypeNotMapped() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.REPAIR);

        underTest.callSendFailedOperationNotificationIfApplicable(stack, "user", operation, "boom");

        verify(eventSenderService, never()).sendEventAndNotification(any(), any(), any(), any());
    }

    @Test
    void updatesStackStatusToUpgradeFailedWhenOperationTypeIsUpgrade() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);

        underTest.callUpdateFailedStackStatusIfApplicable(stack, operation, "boom");

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPGRADE_FAILED, "boom");
    }

    @Test
    void updatesStackStatusToMultiAzMigrationFailedWhenOperationTypeIsMigrateToMultiAz() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.MIGRATE_TO_MULTI_AZ);

        underTest.callUpdateFailedStackStatusIfApplicable(stack, operation, "boom");

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED, "boom");
    }

    @Test
    void doesNotUpdateStackStatusWhenOperationTypeNotMapped() {
        Operation operation = new Operation();
        operation.setOperationType(OperationType.REPAIR);

        underTest.callUpdateFailedStackStatusIfApplicable(stack, operation, "boom");

        verify(stackUpdater, never()).updateStackStatus(any(Stack.class), any(DetailedStackStatus.class), any());
    }

    @Test
    void doesNotUpdateStackStatusWhenOperationIsNull() {
        underTest.callUpdateFailedStackStatusIfApplicable(stack, null, "boom");

        verify(stackUpdater, never()).updateStackStatus(any(Stack.class), any(DetailedStackStatus.class), any());
    }

    private static class TestAction extends AbstractStackAction<DummyState, DummyEvent, CommonContext, DummyPayload> {

        TestAction() {
            super(DummyPayload.class);
        }

        void callSendFailedOperationNotificationIfApplicable(Stack stack, String flowTriggerUserCrn, Operation operation, String errorReason) {
            sendFailedOperationNotificationIfApplicable(stack, flowTriggerUserCrn, operation, errorReason);
        }

        void callUpdateFailedStackStatusIfApplicable(Stack stack, Operation operation, String statusReason) {
            updateFailedStackStatusIfApplicable(stack, operation, statusReason);
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

