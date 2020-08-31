package com.sequenceiq.datalake.flow.create.handler;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_CREATION_FINISHED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
@DisplayName("StackCreationHandler tests")
class StackCreationHandlerTest {

    private static String userId = "userId";

    @Mock
    private ProvisionerService provisionerService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxService sdxService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StackCreationHandler stackCreationHandler;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void acceptTest() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        StackV4Response stackV4Response = new StackV4Response();
        when(provisionerService.waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class))).thenReturn(stackV4Response);
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(sdxService, times(1)).updateRuntimeVersionFromStackResponse(any(), eq(stackV4Response));
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        Assertions.assertEquals("StackCreationSuccessEvent", eventNotified);
        Assertions.assertEquals(StackCreationSuccessEvent.class, event.getData().getClass());
        Assertions.assertEquals(stackId, ((StackCreationSuccessEvent) event.getData()).getResourceId());
        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(STACK_CREATION_FINISHED,
                "Datalake stack created", (Long) stackId);
    }

    @Test
    void acceptTestPollerStackFailed() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new UserBreakException("stack failed")).when(provisionerService)
                .waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        Assertions.assertEquals("SdxCreateFailedEvent", eventNotified);
        Assertions.assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        Assertions.assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        Assertions.assertEquals(UserBreakException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }

    @Test
    void acceptTestPollerStackTimeout() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new PollerStoppedException("stack timeout")).when(provisionerService)
                .waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        Assertions.assertEquals("SdxCreateFailedEvent", eventNotified);
        Assertions.assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        Assertions.assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        Assertions.assertEquals(PollerStoppedException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }

    @Test
    void acceptTestPollerStackOtherError() {
        long stackId = 2L;
        StackCreationWaitRequest stackCreationWaitRequest = new StackCreationWaitRequest(stackId, userId);
        Event receivedEvent = new Event<>(stackCreationWaitRequest);
        doThrow(new PollerException("stack error")).when(provisionerService).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        stackCreationHandler.accept(receivedEvent);
        verify(provisionerService, times(1)).waitCloudbreakClusterCreation(eq(stackId), any(PollingConfig.class));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        Assertions.assertEquals("SdxCreateFailedEvent", eventNotified);
        Assertions.assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        Assertions.assertEquals(stackId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        Assertions.assertEquals(PollerException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
    }
}