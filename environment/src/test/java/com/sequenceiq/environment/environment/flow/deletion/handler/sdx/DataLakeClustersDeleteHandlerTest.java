package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClusterDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class DataLakeClustersDeleteHandlerTest {

    private static final long ENV_ID = 123L;

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String ENV_NAME = "envName";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private SdxDeleteService sdxDeleteService;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private Event.Headers headers;

    @InjectMocks
    private DataLakeClustersDeleteHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENV_ID)
                .withResourceCrn(RESOURCE_CRN)
                .withName(ENV_NAME)
                .build();
        EnvironmentDeletionDto build = EnvironmentDeletionDto
                .builder()
                .withId(ENV_ID)
                .withForceDelete(false)
                .withEnvironmentDto(eventDto)
                .build();
        when(environmentDtoEvent.getData()).thenReturn(build);
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Event.Headers.class));
    }

    @Test
    void accept() {
        EnvironmentView environment = new EnvironmentView();
        when(environmentViewService.getById(ENV_ID)).thenReturn(environment);
        underTest.accept(environmentDtoEvent);
        verify(sdxDeleteService).deleteSdxClustersForEnvironment(any(PollingConfig.class), eq(environment), eq(false));
        verify(eventSender).sendEvent(any(EnvDeleteEvent.class), eq(headers));
        verify(eventSender, never()).sendEvent(any(EnvClusterDeleteFailedEvent.class), any());
        EnvDeleteEvent capturedDeleteEvent = (EnvDeleteEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedDeleteEvent.getResourceName()).isEqualTo(ENV_NAME);
        assertThat(capturedDeleteEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(capturedDeleteEvent.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(capturedDeleteEvent.selector()).isEqualTo("FINISH_ENV_CLUSTERS_DELETE_EVENT");
    }

    @Test
    void acceptFail() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentViewService.getById(ENV_ID)).thenThrow(error);
        underTest.accept(environmentDtoEvent);
        verify(sdxDeleteService, never()).deleteSdxClustersForEnvironment(any(), any(), anyBoolean());
        verify(eventSender).sendEvent(any(EnvClusterDeleteFailedEvent.class), eq(headers));
        verify(eventSender, never()).sendEvent(any(EnvDeleteEvent.class), any());
        EnvClusterDeleteFailedEvent capturedDeleteFailedEvent = (EnvClusterDeleteFailedEvent) baseNamedFlowEvent.getValue();
        assertThat(capturedDeleteFailedEvent.getResourceName()).isEqualTo(ENV_NAME);
        assertThat(capturedDeleteFailedEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(capturedDeleteFailedEvent.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(capturedDeleteFailedEvent.selector()).isEqualTo("FAILED_ENV_CLUSTERS_DELETE_EVENT");
        assertThat(capturedDeleteFailedEvent.getException()).isEqualTo(error);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_DATALAKE_CLUSTERS_EVENT");
    }
}
