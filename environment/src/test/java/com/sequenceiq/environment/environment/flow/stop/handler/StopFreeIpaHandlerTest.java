package com.sequenceiq.environment.environment.flow.stop.handler;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    public void shouldStopFreeipaGivenEnvironmentWithoutParent() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);

        underTest.accept(environmentDtoEvent);

        verify(freeIpaPollerService).stopAttachedFreeipaInstances(any(), any());
        verifyEnvStopEvent(environmentDtoEvent);
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

}
