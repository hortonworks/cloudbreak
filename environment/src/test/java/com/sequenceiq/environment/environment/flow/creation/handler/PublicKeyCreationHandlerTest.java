package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class PublicKeyCreationHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    @Mock
    private Headers headers;

    @InjectMocks
    private PublicKeyCreationHandler underTest;

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
        when(environmentDtoEvent.getData()).thenReturn(eventDto);
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEvent.capture(), any(Headers.class));
    }

    @Test
    void acceptEnvironmentNotFound() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verifyEnvCreationEvent();
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(error);

        underTest.accept(environmentDtoEvent);
        verify(eventSender).sendEvent(baseNamedFlowEvent.capture(), headersArgumentCaptor.capture());
        verifyEnvCreationFailedEvent(error);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("CREATE_PUBLICKEY_EVENT");
    }

    private void verifyEnvCreationEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvCreationEvent.class);

        EnvCreationEvent envCreationEvent = (EnvCreationEvent) event;
        assertThat(envCreationEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envCreationEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreationEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreationEvent.selector()).isEqualTo(START_FREEIPA_CREATION_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvCreationFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = baseNamedFlowEvent.getValue();
        assertThat(event).isInstanceOf(EnvCreationFailureEvent.class);

        EnvCreationFailureEvent envCreateFailedEvent = (EnvCreationFailureEvent) event;
        assertThat(envCreateFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envCreateFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreateFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreateFailedEvent.selector()).isEqualTo(FAILED_ENV_CREATION_EVENT.selector());
        assertThat(envCreateFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }
}
