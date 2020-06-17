package com.sequenceiq.environment.environment.flow.start.handler;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.bus.Event;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartFreeIpaHandlerTest {

    private static final String MOCK_ENV_CRN = "someCrnValue";

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
        when(mockEnvironmentDtoEvent.getHeaders()).thenReturn(mockEventHeaders);

        underTest = new StartFreeIpaHandler(mockEventSender, mockFreeIpaPollerService, mockFreeIpaService);
    }

    @Test
    void testWhenFreeIpaDescribeTellsFreeIpaIsNotStartableThenExceptionComesAndNoStartEventHappens() {
        when(mockFreeIpaService.describe(MOCK_ENV_CRN)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));
        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.DELETED_ON_PROVIDER_SIDE);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(mockEventSender, never()).sendEvent(any(EnvStartEvent.class), any());
        verify(mockEventSender, times(1)).sendEvent(any(EnvStartFailedEvent.class), eq(mockEventHeaders));
    }

    @Test
    void testWhenFreeIpaDescribeTellsFreeIpaIsStartableThenStartEventHappens() {
        when(mockFreeIpaService.describe(MOCK_ENV_CRN)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));
        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.STOPPED);

        underTest.accept(mockEnvironmentDtoEvent);

        verify(mockEventSender, times(1)).sendEvent(any(EnvStartEvent.class), eq(mockEventHeaders));
        verify(mockEventSender, never()).sendEvent(any(EnvStartFailedEvent.class), eq(mockEventHeaders));
    }

}