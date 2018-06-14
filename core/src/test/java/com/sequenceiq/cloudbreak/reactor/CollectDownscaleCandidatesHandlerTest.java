package com.sequenceiq.cloudbreak.reactor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@RunWith(MockitoJUnitRunner.class)
public class CollectDownscaleCandidatesHandlerTest {

    private Long stackId = 11L;

    private Long privateId = 1342L;

    private String hostGroupName = "master";

    private Integer scalingAdjustment = -1;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private AmbariDecommissioner ambariDecommissioner;

    @InjectMocks
    private CollectDownscaleCandidatesHandler testedClass;

    @Test
    public void testFlowWithPrivateIds() throws CloudbreakException {
        //given
        Stack stack = generateStackData();
        when(stackService.getByIdWithLists(stackId)).thenReturn(stack);
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(privateId));
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        verify(ambariDecommissioner, never()).collectDownscaleCandidates(stack, hostGroupName, scalingAdjustment);

    }

    @Test
    public void downScaleNotForced() throws CloudbreakSecuritySetupException {
        //given
        Stack stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(privateId));
        when(stackService.getByIdWithLists(stackId)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertFalse(submittedResult.getRequest().getDetails().isForced());
        verify(ambariDecommissioner).verifyNodesAreRemovable(eq(stack), anyList());
    }

    @Test
    public void downScaleForcedForced() throws CloudbreakSecuritySetupException {
        //given
        Stack stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(privateId), new ClusterDownscaleDetails(true));
        when(stackService.getByIdWithLists(stackId)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertTrue(submittedResult.getRequest().getDetails().isForced());
        verify(ambariDecommissioner, never()).verifyNodesAreRemovable(eq(stack), anyList());
    }

    private Stack generateStackData() {
        return new Stack();
    }

    private Event<CollectDownscaleCandidatesRequest> generateTestDataEvent(Set<Long> privateIds) {
        CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(stackId, hostGroupName, scalingAdjustment, privateIds,
                new ClusterDownscaleDetails());
        return new Event<>(request);
    }

    private Event<CollectDownscaleCandidatesRequest> generateTestDataEvent(Set<Long> privateIds, ClusterDownscaleDetails details) {
        CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(stackId, hostGroupName, scalingAdjustment, privateIds, details);
        return new Event<>(request);
    }
}