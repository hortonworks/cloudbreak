package com.sequenceiq.environment.environment.flow.start.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentStartDto;
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
    private Event<EnvironmentStartDto> mockEnvironmentDtoEvent;

    @Mock
    private EnvironmentStartDto mockEnvironmentDto;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private DescribeFreeIpaResponse mockDescribeFreeIpaResponse;

    @Mock
    private Event.Headers mockEventHeaders;

    @Captor
    private ArgumentCaptor<EnvStartEvent> envStartEventCaptor;

    @Captor
    private ArgumentCaptor<EnvStartFailedEvent> envStartFailedEventCaptor;

    private StartFreeIpaHandler underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mockEnvironmentDtoEvent.getData()).thenReturn(mockEnvironmentDto);
        when(environmentDto.getResourceCrn()).thenReturn(MOCK_ENV_CRN);
        when(environmentDto.getId()).thenReturn(ENV_ID);
        when(mockEnvironmentDto.getEnvironmentDto()).thenReturn(environmentDto);
        when(mockEnvironmentDtoEvent.getHeaders()).thenReturn(mockEventHeaders);

        underTest = new StartFreeIpaHandler(mockEventSender, mockFreeIpaPollerService, mockFreeIpaService);
    }

    @Test
    public void shouldStopFreeipaButStatusIsNull() {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentStartDto> environmentDtoEvent = Event.wrap(mockEnvironmentDto);

        when(mockFreeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verifyEnvStartFailedEvent(environmentDtoEvent, "FreeIPA status is unpredictable, env start will be interrupted.");
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
        assertThat(envStartFailedEvent.getEnvironmentDto()).isEqualTo(environmentDto);
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

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "MAINTENANCE_MODE_ENABLED", "START_IN_PROGRESS"})
    public void shouldSkipStartFreeipaInstanceWhenStatusAvaialbleOrStartInrogress(Status status) {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentStartDto> environmentDtoEvent = Event.wrap(mockEnvironmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(status);
        when(mockFreeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verify(mockFreeIpaPollerService, never()).stopAttachedFreeipaInstances(environmentDto.getId(), envCrn);
        verifyEnvStartEvent(environmentDtoEvent);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"STOPPED", "STOP_FAILED", "START_FAILED", "AVAILABLE", "START_IN_PROGRESS", "MAINTENANCE_MODE_ENABLED"},
            mode = EnumSource.Mode.EXCLUDE)
    public void shouldThrowErrorWhenUnstartable(Status status) {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentStartDto> environmentDtoEvent = Event.wrap(mockEnvironmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(status);
        when(mockFreeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verifyEnvStartFailedEvent(environmentDtoEvent, "FreeIPA is not in a valid state to start! Current state is: " + status);
    }

    private EnvironmentDto createEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(123L);
        environmentDto.setName("name");
        return environmentDto;
    }

    private void verifyEnvStartEvent(Event<EnvironmentStartDto> environmentDtoEvent) {
        verify(mockEventSender).sendEvent(envStartEventCaptor.capture(), eq(environmentDtoEvent.getHeaders()));
        Assertions.assertThat(envStartEventCaptor.getValue())
                .returns(EnvStartStateSelectors.ENV_START_DATALAKE_EVENT.selector(), EnvStartEvent::selector);
    }

    private void verifyEnvStartFailedEvent(Event<EnvironmentStartDto> environmentDtoEvent, String message) {
        verify(mockEventSender).sendEvent(envStartFailedEventCaptor.capture(), eq(environmentDtoEvent.getHeaders()));
        Assertions.assertThat(envStartFailedEventCaptor.getValue())
                .returns(EnvStartStateSelectors.FAILED_ENV_START_EVENT.selector(), EnvStartFailedEvent::selector)
                .returns(EnvironmentStatus.START_FREEIPA_FAILED, EnvStartFailedEvent::getEnvironmentStatus)
                .returns(message, event -> event.getException().getMessage());
    }

}
