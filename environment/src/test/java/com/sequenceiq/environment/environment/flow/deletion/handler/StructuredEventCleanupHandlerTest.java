package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.CLEANUP_EVENTS_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.cleanup.EnvironmentStructuredEventCleanupService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class StructuredEventCleanupHandlerTest {

    @Mock
    private Event.Headers mockHeaders;

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private EnvironmentDeletionDto mockEnvironmentDeletionDto;

    @Mock
    private Event<EnvironmentDeletionDto> mockEventOfEnvironmentDeletionDto;

    @Mock
    private HandlerExceptionProcessor mockHandlerExceptionProcessor;

    @Mock
    private EventSender mockEventSender;

    @Mock
    private EnvironmentStructuredEventCleanupService mockEnvironmentStructuredEventCleanupService;

    private StructuredEventCleanupHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StructuredEventCleanupHandler(mockEventSender, mockHandlerExceptionProcessor, mockEnvironmentStructuredEventCleanupService);
        when(mockEnvironmentDto.getResourceCrn()).thenReturn(TestConstants.ENV_CRN);
        when(mockEnvironmentDeletionDto.getEnvironmentDto()).thenReturn(mockEnvironmentDto);
        when(mockEventOfEnvironmentDeletionDto.getData()).thenReturn(mockEnvironmentDeletionDto);
        when(mockEventOfEnvironmentDeletionDto.getHeaders()).thenReturn(mockHeaders);
    }

    @Test
    void testAcceptWhenEventCleanupFailsThenItShouldBeDoneItSilently() {
        doThrow(new RuntimeException()).when(mockEnvironmentStructuredEventCleanupService).cleanUpStructuredEvents(TestConstants.ENV_CRN);

        underTest.accept(mockEventOfEnvironmentDeletionDto);

        validateMockInteractions();
    }

    @Test
    void mockTestWhenCleanupDoesNotFailThenEverythingShouldGoWell() {
        underTest.accept(mockEventOfEnvironmentDeletionDto);

        validateMockInteractions();
    }

    @Test
    void testWhenSomethingThrowsExceptionOtherThanTheActualActionThenExceptionProcessorWhouldHandleIt() {
        doThrow(new RuntimeException()).when(mockEventSender).sendEvent(any(EnvDeleteEvent.class), any(Event.Headers.class));

        underTest.accept(mockEventOfEnvironmentDeletionDto);

        verify(mockHandlerExceptionProcessor, times(1)).handle(any(HandlerFailureConjoiner.class), any(Logger.class),
                eq(mockEventSender), eq(CLEANUP_EVENTS_EVENT.selector()));
    }

    private void validateMockInteractions() {
        verify(mockEnvironmentStructuredEventCleanupService, times(1)).cleanUpStructuredEvents(TestConstants.ENV_CRN);
        verifyNoMoreInteractions(mockEnvironmentStructuredEventCleanupService);
        verify(mockEventSender, times(1)).sendEvent(any(EnvDeleteEvent.class), eq(mockHeaders));
        verifyNoInteractions(mockHandlerExceptionProcessor);
    }

}