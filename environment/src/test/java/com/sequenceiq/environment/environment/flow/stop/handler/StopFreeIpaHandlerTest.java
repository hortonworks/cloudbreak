package com.sequenceiq.environment.environment.flow.stop.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StopFreeIpaHandlerTest {

    @Mock
    private EventSender eventSender;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private DescribeFreeIpaResponse mockDescribeFreeIpaResponse;

    @InjectMocks
    private StopFreeIpaHandler underTest;

    @Captor
    private ArgumentCaptor<EnvStopEvent> envStopEventCaptor;

    @Captor
    private ArgumentCaptor<EnvStopFailedEvent> envStopFailedEventCaptor;

    @Test
    public void shouldStopFreeipaGivenEnvironmentWithoutParent() {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.AVAILABLE);
        when(freeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verify(freeIpaPollerService).stopAttachedFreeipaInstances(any(), any());
        verifyEnvStopEvent(environmentDtoEvent);
    }

    @Test
    public void shouldStopFreeipaButStatusIsNull() {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        when(freeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verifyEnvStopFailedEvent(environmentDtoEvent, "FreeIPA status is unpredictable, env stop will be interrupted.");
    }

    @Test
    public void shouldNotStopFreeipaGivenEnvironmentWithParent() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setParentEnvironmentCrn("crn:parent");
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        underTest.accept(environmentDtoEvent);

        verify(freeIpaPollerService, times(0)).stopAttachedFreeipaInstances(any(), any());
        verifyEnvStopEvent(environmentDtoEvent);
    }

    @Test
    public void testWhenFreeIpaDescribeTellsFreeIpaIsNotStoppableThenExceptionComesAndNoStopEventHappens() {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(Status.DELETED_ON_PROVIDER_SIDE);
        when(freeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verify(freeIpaService, times(1)).describe(any());
        verify(freeIpaService, times(1)).describe(envCrn);
        verify(eventSender, times(1)).sendEvent(any(), any());
        verify(eventSender, times(1)).sendEvent(any(EnvStopFailedEvent.class), eq(environmentDtoEvent.getHeaders()));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"STOPPED", "STOP_IN_PROGRESS"})
    public void shouldSkipStopFreeipaInstanceWhenStatusStopped(Status status) {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(status);
        when(freeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verify(freeIpaPollerService, never()).stopAttachedFreeipaInstances(environmentDto.getId(), envCrn);
        verifyEnvStopEvent(environmentDtoEvent);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"AVAILABLE", "STOP_FAILED", "START_FAILED", "STOP_IN_PROGRESS", "STOPPED"}, mode = EnumSource.Mode.EXCLUDE)
    public void shouldThrowErrorWhenUnstoppable(Status status) {
        String envCrn = "someCrnValue";
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setResourceCrn(envCrn);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        when(mockDescribeFreeIpaResponse.getStatus()).thenReturn(status);
        when(freeIpaService.describe(envCrn)).thenReturn(Optional.of(mockDescribeFreeIpaResponse));

        underTest.accept(environmentDtoEvent);

        verifyEnvStopFailedEvent(environmentDtoEvent, "FreeIPA is not in a stoppable state! Current state is: " + status);
    }

    private EnvironmentDto createEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(123L);
        environmentDto.setName("name");
        return environmentDto;
    }

    private void verifyEnvStopEvent(Event<EnvironmentDto> environmentDtoEvent) {
        verify(eventSender).sendEvent(envStopEventCaptor.capture(), eq(environmentDtoEvent.getHeaders()));
        Assertions.assertThat(envStopEventCaptor.getValue())
                .returns(EnvStopStateSelectors.FINISH_ENV_STOP_EVENT.selector(), EnvStopEvent::selector)
                .returns(environmentDtoEvent.getData().getId(), EnvStopEvent::getResourceId)
                .returns(environmentDtoEvent.getData().getName(), EnvStopEvent::getResourceName);
    }

    private void verifyEnvStopFailedEvent(Event<EnvironmentDto> environmentDtoEvent, String message) {
        verify(eventSender).sendEvent(envStopFailedEventCaptor.capture(), eq(environmentDtoEvent.getHeaders()));
        Assertions.assertThat(envStopFailedEventCaptor.getValue())
                .returns(EnvStopStateSelectors.FAILED_ENV_STOP_EVENT.selector(), EnvStopFailedEvent::selector)
                .returns(environmentDtoEvent.getData().getId(), EnvStopFailedEvent::getResourceId)
                .returns(environmentDtoEvent.getData().getName(), EnvStopFailedEvent::getResourceName)
                .returns(EnvironmentStatus.STOP_FREEIPA_FAILED, EnvStopFailedEvent::getEnvironmentStatus)
                .returns(message, event -> event.getException().getMessage());
    }

}
