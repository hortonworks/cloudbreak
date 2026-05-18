package com.sequenceiq.freeipa.flow.freeipa.downscale.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DOWNSCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DOWNSCALE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_DOWNSCALE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction.CHAINED_ACTION;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class FreeIpaDownscaleActionsNotificationTest {

    @InjectMocks
    private FreeIpaDownscaleActions underTest;

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
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private StackUpdater stackUpdater;

    private StackContext context;

    @BeforeEach
    void setUp() {
        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void sendsUpgradeNotificationWhenDownscaleFailureHappensDuringUpgradeOperation() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        DownscaleFailureEvent payload = new DownscaleFailureEvent(1L, "phase", Set.of(), Map.of(), new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation(any(), any(), any(), any(), any());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractDownscaleAction<DownscaleFailureEvent> action =
                (AbstractDownscaleAction<DownscaleFailureEvent>) underTest.downscaleFailureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_DOWNSCALE_FAILED, List.of("scale", "boom"));
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_FAILED, List.of("boom"));
    }

    @Test
    void startingDownscaleActionSendsStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        DownscaleEvent payload = new DownscaleEvent("selector", 1L, List.of("i-1"), 2, false, false, false, "op-1");
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId("i-1");
        instanceMetaData.setDiscoveryFQDN("master0");
        doReturn(List.of(instanceMetaData)).when(stack).getAllInstanceMetaDataList();
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractDownscaleAction<DownscaleEvent> action =
                (AbstractDownscaleAction<DownscaleEvent>) underTest.startingDownscaleAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_DOWNSCALE_STARTED,
                List.of("scale", "1", "master0"));
    }

    @Test
    void downscaleFinishedActionSendsFinishedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put("DOWNSCALE_HOSTS", List.of("master0"));
        variables.put(CHAINED_ACTION, true);
        StackEvent payload = new StackEvent("selector", 1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractDownscaleAction<StackEvent> action =
                (AbstractDownscaleAction<StackEvent>) underTest.downscaleFinsihedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_DOWNSCALE_FINISHED,
                List.of("scale", "1", "master0"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, "jobService", freeipaJobService, FreeipaJobService.class);
    }
}

