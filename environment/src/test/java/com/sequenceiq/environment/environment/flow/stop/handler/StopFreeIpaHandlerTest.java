package com.sequenceiq.environment.environment.flow.stop.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class StopFreeIpaHandlerTest {

    @Mock
    private EventSender eventSender;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

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
