package com.sequenceiq.environment.environment.flow.start.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.service.datahub.DatahubPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

class StartDatahubHandlerTest {

    @Mock
    private DatahubPollerService datahubPollerService;

    @Mock
    private EventSender mockEventSender;

    @Captor
    private ArgumentCaptor<EnvStartEvent> envStartEventCaptor;

    @Captor
    private ArgumentCaptor<EnvStartFailedEvent> envStartFailedEventCaptor;

    private StartDatahubHandler underTest;

    @BeforeEach
    public void setupTest() {
        MockitoAnnotations.openMocks(this);
        underTest = new StartDatahubHandler(mockEventSender, datahubPollerService);
    }

    @Test
    void testEventSentAndAttachedStarted() {
        Event<EnvironmentStartDto> startEvent = getEnvironmentStartDtoEvent(DataHubStartAction.START_ALL);
        underTest.accept(startEvent);
        verify(datahubPollerService, times(1)).startAttachedDatahubClusters(any(), any());
        ArgumentCaptor<EnvStartEvent> startEventArgumentCaptor = ArgumentCaptor.forClass(EnvStartEvent.class);
        verify(mockEventSender, times(1)).sendEvent(startEventArgumentCaptor.capture(), any());
        EnvStartEvent startEventArgumentCaptorValue = startEventArgumentCaptor.getValue();
        assertThat(startEventArgumentCaptorValue.getResourceId()).isEqualTo(1L);
    }

    @Test
    void testEventSentAndNoAttachedStarted() {
        Event<EnvironmentStartDto> startEvent = getEnvironmentStartDtoEvent(DataHubStartAction.DO_NOT_START);
        underTest.accept(startEvent);
        verify(datahubPollerService, times(0)).startAttachedDatahubClusters(any(), any());
        verify(mockEventSender, times(1)).sendEvent(any(), any());
    }

    @Test
    void testFailureEvent() {
        Event<EnvironmentStartDto> startEvent = getEnvironmentStartDtoEvent(DataHubStartAction.START_ALL);
        Mockito.doThrow(NullPointerException.class).when(datahubPollerService).startAttachedDatahubClusters(any(), any());
        underTest.accept(startEvent);
        ArgumentCaptor<EnvStartFailedEvent> startFailedEventCaptor = ArgumentCaptor.forClass(EnvStartFailedEvent.class);
        verify(mockEventSender, times(1)).sendEvent(startFailedEventCaptor.capture(), any());
        verify(datahubPollerService, times(1)).startAttachedDatahubClusters(any(), any());
        EnvStartFailedEvent envStartFailedEvent = startFailedEventCaptor.getValue();
        assertThat(envStartFailedEvent.getEnvironmentStatus()).isEqualTo(EnvironmentStatus.START_DATAHUB_FAILED);
    }


    @NotNull
    private Event<EnvironmentStartDto> getEnvironmentStartDtoEvent(DataHubStartAction startAll) {
        EnvironmentDto edto = new EnvironmentDto();
        EnvironmentStartDto sdto = new EnvironmentStartDto();
        edto.setId(1L);
        edto.setResourceCrn("crn");
        sdto.setEnvironmentDto(edto);
        sdto.setDataHubStart(startAll);
        Event<EnvironmentStartDto> startEvent = new Event<>(sdto);
        return startEvent;
    }
}