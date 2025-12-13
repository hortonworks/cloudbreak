package com.sequenceiq.datalake.flow.create.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.flow.delete.event.SdxDeletionFailedEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionSuccessEvent;
import com.sequenceiq.datalake.flow.delete.event.StackDeletionWaitRequest;
import com.sequenceiq.datalake.flow.delete.handler.StackDeletionHandler;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;

@ExtendWith(MockitoExtension.class)
@DisplayName("StackDeletionHandler tests")
class StackDeletionHandlerTest {

    private static String userId = "userId";

    @Mock
    private ProvisionerService provisionerService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StackDeletionHandler stackDeletionHandler;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void acceptTestStackDeletionSuccess() {
        long id = 2L;
        StackDeletionWaitRequest stackCreationWaitRequest = new StackDeletionWaitRequest(id, userId, true);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doNothing().when(provisionerService).waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        stackDeletionHandler.accept(receivedEvent);

        verify(provisionerService, times(1))
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("StackDeletionSuccessEvent", eventNotified);
        assertEquals(StackDeletionSuccessEvent.class, event.getData().getClass());
        assertEquals(id, ((StackDeletionSuccessEvent) event.getData()).getResourceId());
    }

    @Test
    void acceptTestStackDeletionFailed() {
        long id = 2L;
        StackDeletionWaitRequest stackCreationWaitRequest = new StackDeletionWaitRequest(id, userId, true);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new UserBreakException("stack deletion failed")).when(provisionerService)
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        stackDeletionHandler.accept(receivedEvent);

        verify(provisionerService, times(1))
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxDeletionFailedEvent", eventNotified);
        assertEquals(SdxDeletionFailedEvent.class, event.getData().getClass());
        assertEquals(id, ((SdxDeletionFailedEvent) event.getData()).getResourceId());
    }

    @Test
    void acceptTestPollerStackTimeout() {
        long id = 2L;
        StackDeletionWaitRequest stackCreationWaitRequest = new StackDeletionWaitRequest(id, userId, true);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new PollerStoppedException("stack deletion timeout")).when(provisionerService)
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        stackDeletionHandler.accept(receivedEvent);

        verify(provisionerService, times(1))
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxDeletionFailedEvent", eventNotified);
        assertEquals(SdxDeletionFailedEvent.class, event.getData().getClass());
        assertEquals(id, ((SdxDeletionFailedEvent) event.getData()).getResourceId());
    }

    @Test
    void acceptTestPollerStackOtherError() {
        long id = 2L;
        StackDeletionWaitRequest stackCreationWaitRequest = new StackDeletionWaitRequest(id, userId, true);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new PollerException("stack deletion error")).when(provisionerService)
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        stackDeletionHandler.accept(receivedEvent);

        verify(provisionerService, times(1))
                .waitCloudbreakClusterDeletion(eq(id), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxDeletionFailedEvent", eventNotified);
        assertEquals(SdxDeletionFailedEvent.class, event.getData().getClass());
        assertEquals(id, ((SdxDeletionFailedEvent) event.getData()).getResourceId());
    }
}