package com.sequenceiq.environment.environment.flow.start.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class SynchronizeUsersHandlerTest {

    private static final String ENV_CRN = "someCrnValue";

    private static final long ENV_ID = 100L;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private Event<EnvironmentStartDto> environmentDtoEvent;

    @Mock
    private EnvironmentStartDto environmentDto;

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private DescribeFreeIpaResponse describeFreeIpaResponse;

    @Mock
    private EventSender eventSender;

    @Mock
    private Event.Headers eventHeaders;

    @InjectMocks
    private SynchronizeUsersHandler underTest;

    @BeforeEach
    void setUp() {
        when(environmentDtoEvent.getData()).thenReturn(environmentDto);
        when(environmentDtoEvent.getHeaders()).thenReturn(eventHeaders);
        lenient().when(mockEnvironmentDto.getResourceCrn()).thenReturn(ENV_CRN);
        lenient().when(mockEnvironmentDto.getId()).thenReturn(ENV_ID);
        when(environmentDto.getEnvironmentDto()).thenReturn(mockEnvironmentDto);
    }

    @Test
    void testWhenFreeIpaIsAvailableAndEnabledThenSynchronize() {
        ReflectionTestUtils.setField(underTest, "synchronizeOnStartEnabled", true);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getAvailabilityStatus()).thenReturn(AvailabilityStatus.AVAILABLE);
        when(describeFreeIpaResponse.getStatus()).thenReturn(Status.AVAILABLE);

        underTest.accept(environmentDtoEvent);

        verify(eventSender, never()).sendEvent(any(EnvStartFailedEvent.class), eq(eventHeaders));
        verify(freeIpaPollerService, times(1)).waitForSynchronizeUsers(ENV_ID, ENV_CRN);

        ArgumentCaptor<EnvStartEvent> startEventCaptor = ArgumentCaptor.forClass(EnvStartEvent.class);
        verify(eventSender, times(1)).sendEvent(startEventCaptor.capture(), eq(eventHeaders));
        EnvStartEvent envStartEvent = startEventCaptor.getValue();
        assertThat(envStartEvent.selector()).isEqualTo(EnvStartStateSelectors.FINISH_ENV_START_EVENT.selector());
    }

    @Test
    void testWhenFreeIpaIsAvailableAndDisabledThenDontSynchronize() {
        ReflectionTestUtils.setField(underTest, "synchronizeOnStartEnabled", false);

        underTest.accept(environmentDtoEvent);

        verify(eventSender, never()).sendEvent(any(EnvStartFailedEvent.class), eq(eventHeaders));
        verify(freeIpaPollerService, never()).waitForSynchronizeUsers(any(), any());

        ArgumentCaptor<EnvStartEvent> startEventCaptor = ArgumentCaptor.forClass(EnvStartEvent.class);
        verify(eventSender, times(1)).sendEvent(startEventCaptor.capture(), eq(eventHeaders));
        EnvStartEvent envStartEvent = startEventCaptor.getValue();
        assertThat(envStartEvent.selector()).isEqualTo(EnvStartStateSelectors.FINISH_ENV_START_EVENT.selector());
    }

    @Test
    void testWhenFreeIpaIsNotAvailableThenFail() {
        ReflectionTestUtils.setField(underTest, "synchronizeOnStartEnabled", true);

        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));
        when(describeFreeIpaResponse.getAvailabilityStatus()).thenReturn(AvailabilityStatus.UNAVAILABLE);
        when(describeFreeIpaResponse.getStatus()).thenReturn(Status.STOPPED);

        underTest.accept(environmentDtoEvent);

        verify(eventSender, never()).sendEvent(any(EnvStartEvent.class), eq(eventHeaders));
        verify(freeIpaPollerService, never()).waitForSynchronizeUsers(any(), any());

        ArgumentCaptor<EnvStartFailedEvent> failEventCaptor = ArgumentCaptor.forClass(EnvStartFailedEvent.class);
        verify(eventSender, times(1)).sendEvent(failEventCaptor.capture(), eq(eventHeaders));

        EnvStartFailedEvent failedEvent = failEventCaptor.getValue();
        assertThat(failedEvent.getEnvironmentStatus()).isEqualTo(EnvironmentStatus.START_SYNCHRONIZE_USERS_FAILED);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo(EnvStartHandlerSelectors.SYNCHRONIZE_USERS_HANDLER_EVENT.toString());
    }
}
