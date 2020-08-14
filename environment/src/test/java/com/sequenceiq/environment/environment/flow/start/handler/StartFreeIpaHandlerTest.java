package com.sequenceiq.environment.environment.flow.start.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

class StartFreeIpaHandlerTest {

    private static final String MOCK_ENV_CRN = "someCrnValue";

    private static final long ENV_ID = 100L;

    @Mock
    private FreeIpaPollerService mockFreeIpaPollerService;

    @Mock
    private FreeIpaService mockFreeIpaService;

    @Mock
    private EventSender mockEventSender;

    @Mock
    private Event<EnvironmentDto> mockEnvironmentDtoEvent;

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private DescribeFreeIpaResponse mockDescribeFreeIpaResponse;

    @Mock
    private Event.Headers mockEventHeaders;

    private StartFreeIpaHandler underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mockEnvironmentDtoEvent.getData()).thenReturn(mockEnvironmentDto);
        when(mockEnvironmentDto.getResourceCrn()).thenReturn(MOCK_ENV_CRN);
        when(mockEnvironmentDto.getId()).thenReturn(ENV_ID);
        when(mockEnvironmentDtoEvent.getHeaders()).thenReturn(mockEventHeaders);

        underTest = new StartFreeIpaHandler(mockEventSender, mockFreeIpaPollerService, mockFreeIpaService);
    }

    @Test
    void testWhenFreeIpaDescribeTellsFreeIpaIsNotStartableThenExceptionComesAndNoStartEventHappens() {
        when(mockFreeIpaService.describe(MOCK_ENV_CRN)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));
        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.DELETED_ON_PROVIDER_SIDE);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(mockEventSender, never()).sendEvent(any(EnvStartEvent.class), any());
        verify(mockFreeIpaPollerService, never()).startAttachedFreeipaInstances(any(), any());

        ArgumentCaptor<EnvStartFailedEvent> startFailedEventCaptor = ArgumentCaptor.forClass(EnvStartFailedEvent.class);
        verify(mockEventSender, times(1)).sendEvent(startFailedEventCaptor.capture(), eq(mockEventHeaders));
        EnvStartFailedEvent envStartFailedEvent = startFailedEventCaptor.getValue();
        assertThat(envStartFailedEvent.getEnvironmentDto()).isEqualTo(mockEnvironmentDto);
        assertThat(envStartFailedEvent.getEnvironmentStatus()).isEqualTo(EnvironmentStatus.START_FREEIPA_FAILED);
    }

    @Test
    void testWhenFreeIpaDescribeTellsFreeIpaIsStartableThenStartEventHappens() {
        when(mockFreeIpaService.describe(MOCK_ENV_CRN)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));
        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.STOPPED);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(mockEventSender, never()).sendEvent(any(EnvStartFailedEvent.class), eq(mockEventHeaders));
        verify(mockFreeIpaPollerService, times(1)).startAttachedFreeipaInstances(ENV_ID, MOCK_ENV_CRN);

        ArgumentCaptor<EnvStartEvent> startEventCaptor = ArgumentCaptor.forClass(EnvStartEvent.class);
        verify(mockEventSender, times(1)).sendEvent(startEventCaptor.capture(), eq(mockEventHeaders));
        EnvStartEvent envStartEvent = startEventCaptor.getValue();
        assertThat(envStartEvent.selector()).isEqualTo(EnvStartStateSelectors.ENV_START_DATALAKE_EVENT.selector());
    }

}
