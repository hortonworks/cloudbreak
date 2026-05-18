package com.sequenceiq.freeipa.flow.freeipa.verticalscale.actions;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_VERTICAL_SCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_VERTICAL_SCALE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_VERTICAL_SCALE_STARTED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class FreeIpaVerticalScaleActionsNotificationTest {

    @InjectMocks
    private FreeIpaVerticalScaleActions underTest;

    @Mock
    private OperationService operationService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaVerticalScaleService freeIPAVerticalScaleService;

    @Mock
    private StackService stackService;

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
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    private StackContext context;

    @BeforeEach
    void setUp() {
        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void stackVerticalScaleSendsStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        FreeIpaVerticalScalingTriggerEvent payload = new FreeIpaVerticalScalingTriggerEvent("selector", 1L, new VerticalScaleRequest());
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScalingTriggerEvent> action =
                (AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScalingTriggerEvent>) underTest.stackVerticalScale();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, stackService, StackService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_VERTICAL_SCALE_STARTED);
    }

    @Test
    void verticalScaleFinishedSendsFinishedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        FreeIpaVerticalScaleResult payload = new FreeIpaVerticalScaleResult(1L, ResourceStatus.CREATED, List.of(), new VerticalScaleRequest());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScaleResult> action =
                (AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScaleResult>) underTest.verticalScaleFinished();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_VERTICAL_SCALE_FINISHED);
    }

    @Test
    void sendsUpgradeNotificationWhenVerticalScaleFailureHappensDuringUpgradeOperation() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        FreeIpaVerticalScaleFailureEvent payload =
                new FreeIpaVerticalScaleFailureEvent(1L, "phase", Set.of(), ERROR, Map.of(), new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation("acc", "op-1", "Vertical scale failed with [phase]. boom");
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScaleFailureEvent> action =
                (AbstractFreeIpaVerticalScaleAction<FreeIpaVerticalScaleFailureEvent>) underTest.verticalScaleFailedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_VERTICAL_SCALE_FAILED, List.of("boom"));
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_FAILED, List.of("boom"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, "jobService", freeipaJobService, FreeipaJobService.class);
    }
}

