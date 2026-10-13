package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CHANGE_PRIMARY_GATEWAY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CHANGE_PRIMARY_GATEWAY_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_CHANGE_PRIMARY_GATEWAY_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction.FINAL_CHAIN;
import static com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayContext;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationRequest;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.replication.ChangePrimaryGatewayVerifyReplicationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class ChangePrimaryGatewayActionsNotificationTest {

    private static final String VERIFY_REPLICATION = "VERIFY_REPLICATION";

    @InjectMocks
    private ChangePrimaryGatewayActions underTest;

    @Mock
    private OperationService operationService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FreeipaJobService freeipaJobService;

    @Mock
    private Stack stack;

    @Mock
    private StackUpdater stackUpdater;

    private ChangePrimaryGatewayContext context;

    @BeforeEach
    void setUp() {
        context = new ChangePrimaryGatewayContext(new FlowParameters("flow", "user-crn"), stack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void sendsUpgradeNotificationWhenChangePrimaryGatewayFailureHappensDuringUpgradeOperation() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        ChangePrimaryGatewayFailureEvent payload = new ChangePrimaryGatewayFailureEvent(1L, "phase", Set.of(), Map.of(), new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        doReturn("env-crn").when(stack).getEnvironmentCrn();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation(any(), any(), any(), any(), any());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayFailureEvent> action =
                (AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayFailureEvent>) underTest.failureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CHANGE_PRIMARY_GATEWAY_FAILED,
                List.of("repair-chained", "boom"));
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_FAILED, List.of("boom"));
    }

    @Test
    void startingActionSendsStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        ChangePrimaryGatewayEvent payload = new ChangePrimaryGatewayEvent("selector", 1L, List.of("i-1"), true, "op-1");

        AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent> action =
                (AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent>) underTest.startingAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CHANGE_PRIMARY_GATEWAY_STARTED,
                List.of("repair-final", "i-1"));
    }

    @Test
    void finishedActionSendsFinishedNotificationInChainedMode() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(FINAL_CHAIN, false);
        StackEvent payload = new StackEvent("selector", 1L);

        AbstractChangePrimaryGatewayAction<StackEvent> action =
                (AbstractChangePrimaryGatewayAction<StackEvent>) underTest.finsihedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_CHANGE_PRIMARY_GATEWAY_FINISHED,
                List.of("repair-chained"));
    }

    @Test
    void startingActionStoresVerifyReplicationFlag() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        ChangePrimaryGatewayEvent payload = new ChangePrimaryGatewayEvent("selector", 1L, List.of("i-1"), true, "op-1")
                .withReplicationVerification();

        AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent> action =
                (AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent>) underTest.startingAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        assertEquals(Boolean.TRUE, variables.get(VERIFY_REPLICATION));
    }

    @Test
    void startingActionStoresFalseVerifyReplicationFlagByDefault() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        ChangePrimaryGatewayEvent payload = new ChangePrimaryGatewayEvent("selector", 1L, List.of("i-1"), true, "op-1");

        AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent> action =
                (AbstractChangePrimaryGatewayAction<ChangePrimaryGatewayEvent>) underTest.startingAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        assertEquals(Boolean.FALSE, variables.get(VERIFY_REPLICATION));
    }

    @Test
    void verifyReplicationActionSendsRequestAndUpdatesStatusWhenVerificationIsRequested() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(VERIFY_REPLICATION, true);
        StackEvent payload = new StackEvent("selector", 1L);

        AbstractChangePrimaryGatewayAction<StackEvent> action = (AbstractChangePrimaryGatewayAction<StackEvent>) underTest.verifyReplicationAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.REPAIR_IN_PROGRESS, "Verifying FreeIPA replication convergence");
        ChangePrimaryGatewayVerifyReplicationRequest sent = captureSentEvent(ChangePrimaryGatewayVerifyReplicationRequest.class);
        assertEquals(1L, sent.getResourceId());
        verify(eventBus).notify(eq(EventSelectorUtil.selector(ChangePrimaryGatewayVerifyReplicationRequest.class)), any());
    }

    @Test
    void verifyReplicationActionSendsSuccessWithoutStatusUpdateWhenVerificationIsNotRequested() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(VERIFY_REPLICATION, false);
        StackEvent payload = new StackEvent("selector", 1L);

        AbstractChangePrimaryGatewayAction<StackEvent> action = (AbstractChangePrimaryGatewayAction<StackEvent>) underTest.verifyReplicationAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verifyNoInteractions(stackUpdater);
        ChangePrimaryGatewayVerifyReplicationSuccess sent = captureSentEvent(ChangePrimaryGatewayVerifyReplicationSuccess.class);
        assertEquals(1L, sent.getResourceId());
        verify(eventBus).notify(eq(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT.event()), any());
    }

    @Test
    void verifyReplicationActionSendsSuccessWithoutStatusUpdateWhenFlagIsMissing() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        StackEvent payload = new StackEvent("selector", 1L);

        AbstractChangePrimaryGatewayAction<StackEvent> action = (AbstractChangePrimaryGatewayAction<StackEvent>) underTest.verifyReplicationAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verifyNoInteractions(stackUpdater);
        captureSentEvent(ChangePrimaryGatewayVerifyReplicationSuccess.class);
        verify(eventBus).notify(eq(CHANGE_PRIMARY_GATEWAY_VERIFY_REPLICATION_FINISHED_EVENT.event()), any());
    }

    private <T> T captureSentEvent(Class<T> expectedType) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(any(), payloadCaptor.capture());
        return assertInstanceOf(expectedType, payloadCaptor.getValue());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, "jobService", freeipaJobService, FreeipaJobService.class);
    }
}

