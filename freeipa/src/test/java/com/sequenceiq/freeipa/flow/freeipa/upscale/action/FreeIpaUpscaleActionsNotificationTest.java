package com.sequenceiq.freeipa.flow.freeipa.upscale.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPSCALE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPSCALE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPSCALE_STARTED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction.CHAINED_ACTION;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.InstanceGroupAttributeAndStackTemplateUpdater;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class FreeIpaUpscaleActionsNotificationTest {

    @InjectMocks
    private FreeIpaUpscaleActions underTest;

    @Mock
    private OperationService operationService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private EnvironmentService environmentService;

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
    private FreeIpaFailedFlowAnalyzer freeIpaFailedFlowAnalyzer;

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

    @Mock
    private InstanceGroupAttributeAndStackTemplateUpdater instanceGroupAttributeAndStackTemplateUpdater;

    private StackContext context;

    @BeforeEach
    void setUp() {
        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);
        doReturn(1L).when(stack).getId();
    }

    @Test
    void startingActionSendsUpscaleStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        UpscaleEvent payload = new UpscaleEvent("selector", 1L, new java.util.ArrayList<>(), 2, false, false, false, "op-1", null);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractUpscaleAction<UpscaleEvent> action = (AbstractUpscaleAction<UpscaleEvent>) underTest.startingAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, "instanceGroupAttributeAndStackTemplateUpdater", instanceGroupAttributeAndStackTemplateUpdater);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPSCALE_STARTED, List.of("scale", "2"));
    }

    @Test
    void upscaleFinishedActionSendsUpscaleFinishedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put("UPSCALE_HOSTS", List.of("master0"));
        variables.put(CHAINED_ACTION, true);
        StackEvent payload = new StackEvent("selector", 1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractUpscaleAction<StackEvent> action = (AbstractUpscaleAction<StackEvent>) underTest.upscaleFinsihedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPSCALE_FINISHED,
                List.of("scale", "1", "master0"));
    }

    @Test
    void sendsUpgradeNotificationWhenUpscaleFailureHappensDuringUpgradeOperation() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        variables.put("REPAIR", true);
        UpscaleFailureEvent payload = new UpscaleFailureEvent(1L, "phase", Set.of(), ERROR, Map.of(), new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        doReturn("env-crn").when(stack).getEnvironmentCrn();
        doReturn(Set.of()).when(stack).getNotDeletedInstanceMetaDataSet();
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation(any(), any(), any(), any(), any());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractUpscaleAction<UpscaleFailureEvent> action = (AbstractUpscaleAction<UpscaleFailureEvent>) underTest.upscaleFailureAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);
        ReflectionTestUtils.setField(action, null, instanceMetaDataService, InstanceMetaDataService.class);
        ReflectionTestUtils.setField(action, null, environmentService, EnvironmentService.class);
        ReflectionTestUtils.setField(action, null, freeIpaFailedFlowAnalyzer, FreeIpaFailedFlowAnalyzer.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPSCALE_FAILED, List.of("repair", "boom"));
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
