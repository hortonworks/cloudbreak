package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.core.cluster.ClusterStartHandlerService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class ClusterStartHandlerTest {
    @Mock
    private StackService stackService;

    @Mock
    private ClusterStartHandlerService clusterStartHandlerService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ClusterStartHandler underTest;

    @Test
    void testAccept() throws Exception {
        // GIVEN
        ClusterStartRequest clusterStartRequest = new ClusterStartRequest(1L);
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(stackService.getByIdWithListsInTransaction(clusterStartRequest.getStackId())).thenReturn(stack);
        when(clusterStartHandlerService.getCmTemplateProcessor(stack)).thenReturn(cmTemplateProcessor);
        // WHEN
        underTest.accept(Event.wrap(clusterStartRequest));
        // THEN
        verify(clusterStartHandlerService).startCluster(stack, cmTemplateProcessor, false);
        verify(clusterStartHandlerService).handleStopStartScalingFeature(stack, cmTemplateProcessor);
        ArgumentCaptor<Event<ClusterStartResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(ClusterStartResult.class)), argumentCaptor.capture());
        Event<ClusterStartResult> event = argumentCaptor.getValue();
        assertEquals(event.getData().getRequest(), clusterStartRequest);
        assertNull(event.getData().getRequest().getException());
    }

    @Test
    void testAcceptThrowsException() throws Exception {
        // GIVEN
        ClusterStartRequest clusterStartRequest = new ClusterStartRequest(1L);
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        Exception exception = new Exception("exception");
        when(stackService.getByIdWithListsInTransaction(clusterStartRequest.getStackId())).thenReturn(stack);
        when(clusterStartHandlerService.getCmTemplateProcessor(stack)).thenReturn(cmTemplateProcessor);
        doThrow(exception).when(clusterStartHandlerService).startCluster(stack, cmTemplateProcessor, false);
        // WHEN
        underTest.accept(Event.wrap(clusterStartRequest));
        // THEN
        verify(clusterStartHandlerService).startCluster(stack, cmTemplateProcessor, false);
        verify(clusterStartHandlerService, never()).handleStopStartScalingFeature(stack, cmTemplateProcessor);
        ArgumentCaptor<Event<ClusterStartResult>> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(eq(EventSelectorUtil.failureSelector(ClusterStartResult.class)), argumentCaptor.capture());
        Event<ClusterStartResult> event = argumentCaptor.getValue();
        assertEquals(clusterStartRequest, event.getData().getRequest());
        assertEquals(exception, event.getData().getErrorDetails());
    }
}
