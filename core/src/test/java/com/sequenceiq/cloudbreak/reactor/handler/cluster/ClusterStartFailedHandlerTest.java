package com.sequenceiq.cloudbreak.reactor.handler.cluster;

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

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartFailedRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class ClusterStartFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @InjectMocks
    private ClusterStartFailedHandler underTest;

    @Test
    public void testHandleClusterStartFailedRequest() {
        ClusterStartFailedRequest request = new ClusterStartFailedRequest(STACK_ID);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));
        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterStartEvent.FAIL_HANDLED_EVENT.event());

        verify(conclusionCheckerService, times(1)).runConclusionChecker(eq(STACK_ID), anyString(), any());
    }
}