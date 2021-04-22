package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import org.slf4j.Logger;

import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class PublicKeyDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private HandlerExceptionProcessor mockExceptionProcessor;

    @Mock
    private Headers headers;

    @InjectMocks
    private PublicKeyDeleteHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEvent;

    @Captor
    private ArgumentCaptor<Headers> headersArgumentCaptor;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .build();
        EnvironmentDeletionDto build = EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(eventDto)
                .build();
        lenient().when(environmentDtoEvent.getData()).thenReturn(build);
        lenient().when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        lenient().doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Headers.class));
    }

    @Test
    void acceptEnvironmentNotFound() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(error);

        underTest.accept(environmentDtoEvent);
        verify(mockExceptionProcessor, times(1)).handle(any(), any(), any(), any());
        verify(mockExceptionProcessor, times(1))
                .handle(any(HandlerFailureConjoiner.class), any(Logger.class), eq(eventSender), eq(DELETE_PUBLICKEY_EVENT.selector()));
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_PUBLICKEY_EVENT");
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_NETWORK_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvDeleteFailedEvent.class);

        EnvDeleteFailedEvent envDeleteFailedEvent = (EnvDeleteFailedEvent) event;
        assertThat(envDeleteFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteFailedEvent.selector()).isEqualTo(FAILED_ENV_DELETE_EVENT.selector());
        assertThat(envDeleteFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }
}
