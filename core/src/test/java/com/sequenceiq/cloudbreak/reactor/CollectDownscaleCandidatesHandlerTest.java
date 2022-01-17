package com.sequenceiq.cloudbreak.reactor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class CollectDownscaleCandidatesHandlerTest {

    private static final String HOST_GROUP_NAME = "master";

    private static final Long STACK_ID = 11L;

    private static final Long PRIVATE_ID = 1342L;

    private static final Integer SCALING_ADJUSTMENT = -1;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CollectDownscaleCandidatesHandler testedClass;

    @Test
    public void testFlowWithPrivateIds() {
        //given
        Stack stack = generateStackData();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID));
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);

    }

    @Test
    public void downScaleNotForced() {
        //given
        Stack stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertFalse(submittedResult.getRequest().getDetails().isForced());
    }

    @Test
    public void downScaleForcedForced() {
        //given
        Stack stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID), new ClusterDownscaleDetails(true, false));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify((Object) any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertTrue(submittedResult.getRequest().getDetails().isForced());
    }

    private Stack generateStackData() {
        return new Stack();
    }

    private Event<CollectDownscaleCandidatesRequest> generateTestDataEvent(Set<Long> privateIds) {
        CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(STACK_ID, Map.of(HOST_GROUP_NAME, SCALING_ADJUSTMENT),
                Map.of(HOST_GROUP_NAME, privateIds), new ClusterDownscaleDetails());
        return new Event<>(request);
    }

    private Event<CollectDownscaleCandidatesRequest> generateTestDataEvent(Set<Long> privateIds, ClusterDownscaleDetails details) {
        CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(STACK_ID, Map.of(HOST_GROUP_NAME, SCALING_ADJUSTMENT),
                Map.of(HOST_GROUP_NAME, privateIds), details);
        return new Event<>(request);
    }
}
