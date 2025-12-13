package com.sequenceiq.cloudbreak.reactor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class CollectDownscaleCandidatesHandlerTest {

    private static final String HOST_GROUP_NAME = "master";

    private static final Long STACK_ID = 11L;

    private static final Long PRIVATE_ID = 1342L;

    private static final Integer SCALING_ADJUSTMENT = -1;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private CollectDownscaleCandidatesHandler testedClass;

    @Test
    void testFlowWithPrivateIds() {
        //given
        StackDto stack = generateStackData();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID));
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);

    }

    @Test
    void downScaleNotForced() {
        //given
        StackDto stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertFalse(submittedResult.getRequest().getDetails().isForced());
    }

    @Test
    void downScaleForcedForced() {
        //given
        StackDto stack = generateStackData();
        Event<CollectDownscaleCandidatesRequest> event = generateTestDataEvent(Collections.singleton(PRIVATE_ID),
                new ClusterDownscaleDetails(true, false, false));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        //when
        testedClass.accept(event);
        //then
        ArgumentCaptor<Event<CollectDownscaleCandidatesResult>> capturedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(any(), capturedEvent.capture());
        Event<CollectDownscaleCandidatesResult> submittedEvent = capturedEvent.getValue();
        CollectDownscaleCandidatesResult submittedResult = submittedEvent.getData();
        assertNotNull(submittedResult);
        assertTrue(submittedResult.getRequest().getDetails().isForced());
    }

    private StackDto generateStackData() {
        return mock(StackDto.class);
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
