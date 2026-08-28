package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_STARTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CLUSTER_OPERATION;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPDATE_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDeregistrationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class FreeIpaLoadBalancerDeletionActionsTest {

    @InjectMocks
    private FreeIpaLoadBalancerDeletionActions underTest;

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
    void loadBalancerDnsDeregistrationUpdatesStatusAndSendsStartedNotification() throws Exception {
        LoadBalancerDeletionTriggerEvent payload = new LoadBalancerDeletionTriggerEvent("selector", 1L);

        AbstractLoadBalancerDeletionAction<LoadBalancerDeletionTriggerEvent> action =
                (AbstractLoadBalancerDeletionAction<LoadBalancerDeletionTriggerEvent>) underTest.loadBalancerDnsDeregistration();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION, "Deleting FreeIPA load balancer: removing DNS entry");
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_DELETION_STARTED);
    }

    @Test
    void loadBalancerCloudDeletionUpdatesStatus() throws Exception {
        LoadBalancerDeregistrationSuccess payload = new LoadBalancerDeregistrationSuccess(1L);

        AbstractLoadBalancerDeletionAction<LoadBalancerDeregistrationSuccess> action =
                (AbstractLoadBalancerDeletionAction<LoadBalancerDeregistrationSuccess>) underTest.loadBalancerCloudDeletion();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(stackUpdater).updateStackStatus(stack, CLUSTER_OPERATION, "Deleting FreeIPA load balancer: removing cloud resources");
        verify(eventSenderService, never()).sendEventAndNotification(any(), any(), any());
    }

    @Test
    void loadBalancerDeletionFinishedSendsFinishedNotification() throws Exception {
        LoadBalancerCloudDeletionSuccess payload = new LoadBalancerCloudDeletionSuccess(1L);

        AbstractLoadBalancerDeletionAction<LoadBalancerCloudDeletionSuccess> action =
                (AbstractLoadBalancerDeletionAction<LoadBalancerCloudDeletionSuccess>) underTest.loadBalancerDeletionFinished();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_DELETION_FINISHED);
    }

    @Test
    void loadBalancerDeletionFailedUpdatesStatusWithExceptionMessageAndSendsNotification() throws Exception {
        LoadBalancerDeletionFailureEvent payload = new LoadBalancerDeletionFailureEvent(1L, ERROR, new Exception("boom"));

        AbstractLoadBalancerDeletionAction<LoadBalancerDeletionFailureEvent> action =
                (AbstractLoadBalancerDeletionAction<LoadBalancerDeletionFailureEvent>) underTest.loadBalancerDeletionFailed();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(stackUpdater).updateStackStatus(stack, UPDATE_FAILED, "FreeIPA load balancer deletion failed: boom");
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_DELETION_FAILED, List.of("boom"));
    }

    @Test
    void loadBalancerDeletionFailedWithNullExceptionFallsBackToUnknownError() throws Exception {
        LoadBalancerDeletionFailureEvent payload = new LoadBalancerDeletionFailureEvent(1L, ERROR, null);

        AbstractLoadBalancerDeletionAction<LoadBalancerDeletionFailureEvent> action =
                (AbstractLoadBalancerDeletionAction<LoadBalancerDeletionFailureEvent>) underTest.loadBalancerDeletionFailed();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(stackUpdater).updateStackStatus(stack, UPDATE_FAILED, "FreeIPA load balancer deletion failed: Unknown error");
        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_LOAD_BALANCER_DELETION_FAILED, List.of("Unknown error"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, stackUpdater, StackUpdater.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
    }
}
