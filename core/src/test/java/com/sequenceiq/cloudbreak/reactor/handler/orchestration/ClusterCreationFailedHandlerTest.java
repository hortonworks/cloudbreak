package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterCreationFailedRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterCreationFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @InjectMocks
    private ClusterCreationFailedHandler underTest;

    @Test
    public void testHandleClusterCreationFailedRequestBeforeSaltBootstrap() {
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID, ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP);
        HandlerEvent<ClusterCreationFailedRequest> handlerEvent = new HandlerEvent<>(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.CLUSTER_PROVISION_BEFORE_SALT_BOOTSTRAP));
    }

    @Test
    public void testHandleClusterCreationFailedRequestAfterSaltBootstrap() {
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID, ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP);
        HandlerEvent<ClusterCreationFailedRequest> handlerEvent = new HandlerEvent<>(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.CLUSTER_PROVISION_AFTER_SALT_BOOTSTRAP));
    }

    @Test
    public void testHandleClusterCreationFailedRequestAfterStartingClusterServices() {
        ClusterCreationFailedRequest request = new ClusterCreationFailedRequest(STACK_ID, ConclusionCheckerType.DEFAULT);
        HandlerEvent<ClusterCreationFailedRequest> handlerEvent = new HandlerEvent<>(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any(),
                eq(ConclusionCheckerType.DEFAULT));
    }

    private static StackStatus createStackStatus(DetailedStackStatus detailedStackStatus) {
        return new StackStatus(new Stack(), detailedStackStatus);
    }
}
