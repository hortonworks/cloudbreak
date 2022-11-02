package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterCreationFailedRequest;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ClusterCreationFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private ClusterCreationFailedHandler underTest;

    @Test
    public void testHandleClusterCreationFailedRequestBeforeSaltBootstrap() {
        when(stackStatusService.findAllStackStatusesById(STACK_ID)).thenReturn(List.of(createStackStatus(DetailedStackStatus.REGISTERING_TO_CLUSTER_PROXY)));
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP), any());
    }

    @Test
    public void testHandleClusterCreationFailedRequestAfterSaltBootstrap() {
        when(stackStatusService.findAllStackStatusesById(STACK_ID)).thenReturn(
                List.of(createStackStatus(DetailedStackStatus.REGISTERING_TO_CLUSTER_PROXY), createStackStatus(DetailedStackStatus.COLLECTING_HOST_METADATA)));
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP), any());
    }

    @Test
    public void testHandleClusterCreationFailedRequestAfterStartingClusterServices() {
        when(stackStatusService.findAllStackStatusesById(STACK_ID)).thenReturn(
                List.of(createStackStatus(DetailedStackStatus.REGISTERING_TO_CLUSTER_PROXY), createStackStatus(DetailedStackStatus.COLLECTING_HOST_METADATA),
                        createStackStatus(DetailedStackStatus.STARTING_CLUSTER_SERVICES)));
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.DEFAULT), any());
    }

    private static StackStatus createStackStatus(DetailedStackStatus detailedStackStatus) {
        return new StackStatus(new Stack(), detailedStackStatus);
    }
}