package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_PROVISION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_PROVISION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_PROVISION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_UPGRADE_FAILED;
import static com.sequenceiq.freeipa.flow.OperationAwareAction.OPERATION_ID;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.AbstractLoadBalancerCreationAction.LOAD_BALANCER_PROVISIONING_MODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.common.FreeIpaFailedFlowAnalyzer;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerCreationTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerProvisioningMode;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.update.LoadBalancerDomainUpdateSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerConfigurationService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerProvisionActionsTest {

    @InjectMocks
    private FreeIpaLoadBalancerProvisionActions underTest;

    @Mock
    private FreeIpaFailedFlowAnalyzer freeIpaFailedFlowAnalyzer;

    @Mock
    private OperationService operationService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

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
    void provisionFailedActionCallsFailOperationOnlyOnceInUpgradePath() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        variables.put(LOAD_BALANCER_PROVISIONING_MODE, LoadBalancerProvisioningMode.UPGRADE);
        LoadBalancerCreationFailureEvent payload = new LoadBalancerCreationFailureEvent(1L, ERROR, new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        doReturn("env-crn").when(stack).getEnvironmentCrn();
        doReturn(false).when(freeIpaFailedFlowAnalyzer).isValidationFailedError(any());
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation(eq("acc"), eq("op-1"), eq("boom"), any(), any());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractLoadBalancerCreationAction<LoadBalancerCreationFailureEvent> action =
                (AbstractLoadBalancerCreationAction<LoadBalancerCreationFailureEvent>) underTest.handleLoadBalancerProvisionFailure();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, freeIpaFailedFlowAnalyzer, FreeIpaFailedFlowAnalyzer.class);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(operationService, times(1)).failOperation(eq("acc"), eq("op-1"), eq("boom"), any(), any());
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_PROVISION_FAILED, List.of("boom"));
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_UPGRADE_FAILED, List.of("boom"));
    }

    @Test
    void createConfigurationSendsStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        LoadBalancerCreationTriggerEvent payload =
                new LoadBalancerCreationTriggerEvent("selector", 1L, LoadBalancerProvisioningMode.BOOTSTRAP, "op-1");

        AbstractLoadBalancerCreationAction<LoadBalancerCreationTriggerEvent> action =
                (AbstractLoadBalancerCreationAction<LoadBalancerCreationTriggerEvent>) underTest.createConfiguration();
        initActionPrivateFields(action);
        FreeIpaLoadBalancerService freeIpaLoadBalancerService = org.mockito.Mockito.mock(FreeIpaLoadBalancerService.class);
        ReflectionTestUtils.setField(action, "freeIpaLoadBalancerService", freeIpaLoadBalancerService);
        ReflectionTestUtils.setField(action, "freeIpaLoadBalancerConfigurationService",
                org.mockito.Mockito.mock(FreeIpaLoadBalancerConfigurationService.class));
        doReturn(java.util.Optional.of(org.mockito.Mockito.mock(LoadBalancer.class))).when(freeIpaLoadBalancerService).findByStackId(1L);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_PROVISION_STARTED);
    }

    @Test
    void loadBalancerCreationFinishedSendsFinishedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        LoadBalancerDomainUpdateSuccess payload = new LoadBalancerDomainUpdateSuccess(1L);

        AbstractLoadBalancerCreationAction<LoadBalancerDomainUpdateSuccess> action =
                (AbstractLoadBalancerCreationAction<LoadBalancerDomainUpdateSuccess>) underTest.loadBalancerCreationFinished();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_PROVISION_FINISHED);
    }

    @Test
    void provisionFailedActionCallsFailOperationOnlyOnceInBootstrapPathAndNoUpgradeNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        variables.put(OPERATION_ID, "op-1");
        variables.put(LOAD_BALANCER_PROVISIONING_MODE, LoadBalancerProvisioningMode.BOOTSTRAP);
        LoadBalancerCreationFailureEvent payload = new LoadBalancerCreationFailureEvent(1L, ERROR, new Exception("boom"));
        doReturn("acc").when(stack).getAccountId();
        doReturn("env-crn").when(stack).getEnvironmentCrn();
        doReturn(false).when(freeIpaFailedFlowAnalyzer).isValidationFailedError(any());
        Operation operation = new Operation();
        operation.setOperationType(OperationType.UPGRADE);
        doReturn(operation).when(operationService).failOperation(eq("acc"), eq("op-1"), eq("boom"), any(), any());
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractLoadBalancerCreationAction<LoadBalancerCreationFailureEvent> action =
                (AbstractLoadBalancerCreationAction<LoadBalancerCreationFailureEvent>) underTest.handleLoadBalancerProvisionFailure();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, freeIpaFailedFlowAnalyzer, FreeIpaFailedFlowAnalyzer.class);
        ReflectionTestUtils.setField(action, null, operationService, OperationService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(operationService, times(1)).failOperation(eq("acc"), eq("op-1"), eq("boom"), any(), any());
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_PROVISION_FAILED, List.of("boom"));
        verify(eventSenderService, never()).sendEventAndNotification(eq(stack), eq("user-crn"), eq(FREEIPA_UPGRADE_FAILED), any());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, stackUpdater, StackUpdater.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}

