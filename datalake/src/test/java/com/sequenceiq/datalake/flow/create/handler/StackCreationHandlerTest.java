package com.sequenceiq.datalake.flow.create.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;

@ExtendWith(MockitoExtension.class)
@DisplayName("StackCreationHandler tests")
class StackCreationHandlerTest {

    private static String userId = "userId";

    @Mock
    private ProvisionerService provisionerService;

    @Mock
    private SdxService sdxService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StackCreationHandler stackCreationHandler;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(stackCreationHandler, "sleepTimeInSec", 10);
        ReflectionTestUtils.setField(stackCreationHandler, "durationInMinutes", 240);
        ReflectionTestUtils.setField(stackCreationHandler, "durationInMinutesPrewarmed", 120);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptTest(boolean prewarmed) {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        StackV4Response stackV4Response = new StackV4Response();
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(stackId)).thenReturn(sdxCluster);
        when(sdxService.isPrewarmedImage(sdxCluster)).thenReturn(prewarmed);
        when(provisionerService.waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class))).thenReturn(stackV4Response);
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(sdxService, times(1)).updateRuntimeVersionFromStackResponse(eq(sdxCluster), eq(stackV4Response));
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("StackCreationSuccessEvent", eventNotified);
        assertEquals(StackCreationSuccessEvent.class, event.getData().getClass());
        assertEquals(stackId, ((StackCreationSuccessEvent) event.getData()).getResourceId());
    }

    @Test
    void acceptTestPollerStackFailed() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(stackId)).thenReturn(sdxCluster);
        when(sdxService.isPrewarmedImage(sdxCluster)).thenReturn(true);
        doThrow(new UserBreakException("stack failed")).when(provisionerService)
                .waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxCreateFailedEvent", eventNotified);
        assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        assertEquals(UserBreakException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }

    @Test
    void acceptTestPollerStackTimeout() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(stackId)).thenReturn(sdxCluster);
        when(sdxService.isPrewarmedImage(sdxCluster)).thenReturn(true);
        doThrow(new PollerStoppedException("stack timeout")).when(provisionerService)
                .waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxCreateFailedEvent", eventNotified);
        assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        assertEquals(PollerStoppedException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }

    @Test
    void acceptTestPollerStackOtherError() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(stackId)).thenReturn(sdxCluster);
        when(sdxService.isPrewarmedImage(sdxCluster)).thenReturn(true);
        doThrow(new PollerException("stack error")).when(provisionerService).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxCreateFailedEvent", eventNotified);
        assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        assertEquals(PollerException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptTestPollerTimeoutMessageUsesEffectiveDuration(boolean prewarmed) {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        SdxCluster sdxCluster = new SdxCluster();
        when(sdxService.getById(stackId)).thenReturn(sdxCluster);
        when(sdxService.isPrewarmedImage(sdxCluster)).thenReturn(prewarmed);
        doThrow(new PollerStoppedException("stack timeout")).when(provisionerService)
                .waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(), sentEvent.capture());
        SdxCreateFailedEvent failedEvent = (SdxCreateFailedEvent) sentEvent.getValue().getData();
        assertEquals(PollerStoppedException.class, failedEvent.getException().getClass());
        int expectedDuration = prewarmed ? 120 : 240;
        assertEquals("Data Lake stack creation timed out after " + expectedDuration + " minutes", failedEvent.getException().getMessage());
    }
}
