package com.sequenceiq.datalake.flow.create.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.SdxValidationRequest;
import com.sequenceiq.datalake.flow.create.event.SdxValidationSuccessEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.datalake.service.sdx.SdxRecommendationService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class SdxValidationHandlerTest {

    private static String userId = "userId";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private SdxRecommendationService sdxRecommendationService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private SdxValidationHandler underTest;

    @Test
    public void acceptTestValidationSucceeded() {
        long sdxId = 1L;
        SdxValidationRequest sdxValidationRequest = new SdxValidationRequest(sdxId, userId);
        Event receivedEvent = new Event<>(sdxValidationRequest);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        when(environmentService.waitNetworkAndGetEnvironment(eq(sdxId))).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        when(sdxClusterRepository.findById(eq(sdxId))).thenReturn(Optional.of(sdxCluster));

        underTest.accept(receivedEvent);
        verify(sdxRecommendationService, times(1)).validateVmTypeOverride(eq(detailedEnvironmentResponse), eq(sdxCluster));
        verify(sdxRecommendationService, times(1)).validateRecommendedImage(eq(detailedEnvironmentResponse), eq(sdxCluster));
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxValidationSuccessEvent", eventNotified);
        assertEquals(SdxValidationSuccessEvent.class, event.getData().getClass());
        assertEquals(sdxId, ((SdxValidationSuccessEvent) event.getData()).getResourceId());
    }

    @Test
    public void acceptTestValidationFailed() {
        long sdxId = 1L;
        SdxValidationRequest sdxValidationRequest = new SdxValidationRequest(sdxId, userId);
        Event receivedEvent = new Event<>(sdxValidationRequest);
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        when(environmentService.waitNetworkAndGetEnvironment(eq(sdxId))).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(sdxId);
        when(sdxClusterRepository.findById(eq(sdxId))).thenReturn(Optional.of(sdxCluster));
        doThrow(new BadRequestException("validation failed")).when(sdxRecommendationService).validateVmTypeOverride(any(), any());

        underTest.accept(receivedEvent);
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event event = sentEvent.getValue();
        assertEquals("SdxCreateFailedEvent", eventNotified);
        assertEquals(SdxCreateFailedEvent.class, event.getData().getClass());
        assertEquals(sdxId, ((SdxCreateFailedEvent) event.getData()).getResourceId());
        assertEquals(BadRequestException.class, ((SdxCreateFailedEvent) event.getData()).getException().getClass());
        assertEquals("validation failed", ((SdxCreateFailedEvent) event.getData()).getException().getMessage());
    }
}