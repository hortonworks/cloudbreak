package com.sequenceiq.environment.environment.flow.deletion.handler.datahub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class DataHubClusterDeletionHandlerTest {

    private static final long ENV_ID = 123L;

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ENV_NAME = "envName";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DatahubDeletionService datahubDeletionService;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    @Mock
    private Event.Headers headers;

    @InjectMocks
    private DataHubClusterDeletionHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENV_ID)
                .withResourceCrn(RESOURCE_CRN)
                .withName(ENV_NAME)
                .build();
        when(environmentDtoEvent.getData()).thenReturn(eventDto);
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Event.Headers.class));
    }

    @Test
    void accept() {
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.of(environment));
        underTest.accept(environmentDtoEvent);
        verify(datahubDeletionService).deleteDatahubClustersForEnvironment(any(PollingConfig.class), eq(environment));
        verify(eventSender).sendEvent(any(EnvDeleteEvent.class), eq(headers));
        verify(eventSender, never()).sendEvent(any(EnvDeleteFailedEvent.class), any());
        EnvDeleteEvent capturedDeleteEvent = (EnvDeleteEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedDeleteEvent.getResourceName()).isEqualTo(ENV_NAME);
        assertThat(capturedDeleteEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(capturedDeleteEvent.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(capturedDeleteEvent.selector()).isEqualTo("START_DATALAKE_CLUSTERS_DELETE_EVENT");
    }

    @Test
    void acceptEnvironmentNotFound() {
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.empty());
        underTest.accept(environmentDtoEvent);
        verify(datahubDeletionService, never()).deleteDatahubClustersForEnvironment(any(), any());
        verify(eventSender).sendEvent(any(EnvDeleteEvent.class), eq(headers));
        verify(eventSender, never()).sendEvent(any(EnvDeleteFailedEvent.class), any());
        EnvDeleteEvent capturedDeleteEvent = (EnvDeleteEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedDeleteEvent.getResourceName()).isEqualTo(ENV_NAME);
        assertThat(capturedDeleteEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(capturedDeleteEvent.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(capturedDeleteEvent.selector()).isEqualTo("START_DATALAKE_CLUSTERS_DELETE_EVENT");
    }

    @Test
    void acceptFail() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENV_ID)).thenThrow(error);
        underTest.accept(environmentDtoEvent);
        verify(datahubDeletionService, never()).deleteDatahubClustersForEnvironment(any(), any());
        verify(eventSender).sendEvent(any(EnvDeleteFailedEvent.class), eq(headers));
        verify(eventSender, never()).sendEvent(any(EnvDeleteEvent.class), any());
        EnvDeleteFailedEvent capturedDeleteFailedEvent = (EnvDeleteFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedDeleteFailedEvent.getResourceName()).isEqualTo(ENV_NAME);
        assertThat(capturedDeleteFailedEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(capturedDeleteFailedEvent.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(capturedDeleteFailedEvent.selector()).isEqualTo("FAILED_ENV_DELETE_EVENT");
        assertThat(capturedDeleteFailedEvent.getException()).isEqualTo(error);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_DATAHUB_CLUSTERS_EVENT");
    }
}
