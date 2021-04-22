package com.sequenceiq.environment.environment.flow.deletion.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

class HandlerExceptionProcessorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerExceptionProcessorTest.class);

    private HandlerExceptionProcessor underTest;

    @Mock
    private EventSender mockEventSender;

    @Mock
    private Event<EnvironmentDeletionDto> mockEnvironmentDeletionDtoEvent;

    private HandlerFailureConjoiner conjoiner;

    private EnvironmentDto environmentDto;

    private EnvDeleteEvent envDeleteEvent;

    private EnvironmentDeletionDto environmentDeletionDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        environmentDto = createEnvironmentDto();
        envDeleteEvent = createEnvDeleteEvent();
        environmentDeletionDto = createEnvironmentDeletionDto();

        when(mockEnvironmentDeletionDtoEvent.getData()).thenReturn(environmentDeletionDto);

        conjoiner = combineHandlerFailureConjoiner();

        underTest = new HandlerExceptionProcessor();
    }

    @Test
    void testHandleWhenEnvironmentDeletionDtoContainsTrueForForceDeleteThenEnvDeleteEventShouldBeSent() {
        underTest.handle(conjoiner, LOGGER, mockEventSender, "someSelector");

        verify(mockEventSender, times(1)).sendEvent(any(), any());
        verify(mockEventSender, times(1)).sendEvent(eq(envDeleteEvent), any());
    }

    @Test
    void testHandleWhenEnvironmentDeletionDtoContainsFalseForForceDeleteThenEnvDeleteFailedEventShouldBeSent() {
        environmentDeletionDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withForceDelete(false)
                .withId(1L)
                .build();

        when(mockEnvironmentDeletionDtoEvent.getData()).thenReturn(environmentDeletionDto);

        conjoiner = combineHandlerFailureConjoiner();

        underTest.handle(conjoiner, LOGGER, mockEventSender, "someSelector");

        verify(mockEventSender, times(1)).sendEvent(any(), any());
        verify(mockEventSender, times(1)).sendEvent(any(EnvDeleteFailedEvent.class), any());
    }

    private HandlerFailureConjoiner combineHandlerFailureConjoiner() {
        return new HandlerFailureConjoiner(
                new Exception("someMessage"),
                mockEnvironmentDeletionDtoEvent,
                envDeleteEvent
        );
    }

    private EnvironmentDeletionDto createEnvironmentDeletionDto() {
        return EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withForceDelete(true)
                .withId(1L)
                .build();
    }

    private EnvironmentDto createEnvironmentDto() {
        return EnvironmentDto.builder().build();
    }

    private EnvDeleteEvent createEnvDeleteEvent() {
        return new EnvDeleteEvent("someSelector", 1L, "someResource", "someCrn", true);
    }

}