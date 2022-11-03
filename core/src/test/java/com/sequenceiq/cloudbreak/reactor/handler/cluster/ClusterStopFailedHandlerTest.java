package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopFailedRequest;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterStopFailedHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConclusionCheckerService conclusionCheckerService;

    @InjectMocks
    private ClusterStopFailedHandler underTest;

    @Test
    public void testHandleClusterStopFailedRequest() {
        ClusterStopFailedRequest request = new ClusterStopFailedRequest(STACK_ID);
        HandlerEvent handlerEvent = new HandlerEvent(Event.wrap(request));

        Selectable selectable = underTest.doAccept(handlerEvent);

        assertThat(selectable).isInstanceOf(StackEvent.class);
        assertThat(selectable.getSelector()).isEqualTo(ClusterStopEvent.FINALIZED_EVENT.event());
        verify(conclusionCheckerService, times(1)).runConclusionChecker(anyLong(), anyString(), any(),
                eq(ConclusionCheckerType.DEFAULT), any());
    }
}