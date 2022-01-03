package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_RDBMS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class RdbmsDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private HandlerExceptionProcessor mockExceptionProcessor;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private Event.Headers headers;

    @InjectMocks
    private RdbmsDeleteHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEventCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersArgumentCaptor;

    @Captor
    private ArgumentCaptor<HandlerFailureConjoiner> handlerFailureConjoinerCaptor;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AZURE")
                .build();
        EnvironmentDeletionDto environmentDeletionDto = EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(eventDto)
                .build();
        lenient().when(environmentDtoEvent.getData()).thenReturn(environmentDeletionDto);
        lenient().when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        lenient().doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), any(Event.Headers.class));
    }

    @Test
    void acceptEnvironmentSuccess() {
        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_RDBMS_EVENT");
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(eventSender.sendEvent(any(), any())).thenThrow(error);

        underTest.accept(environmentDtoEvent);

        verify(mockExceptionProcessor).handle(handlerFailureConjoinerCaptor.capture(), any(Logger.class), eq(eventSender),
                eq(DELETE_RDBMS_EVENT.selector()));
        verifyEnvDeleteFailedEvent(error);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        verifyEnvDeleteFailedEvent(exceptionExpected, false, "");
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEventCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected, boolean wrappedInCloudbreakServiceException,
        String messageCloudbreakServiceExceptionExpected) {
        HandlerFailureConjoiner conjoiner = handlerFailureConjoinerCaptor.getValue();

        Exception exception = conjoiner.getException();
        if (wrappedInCloudbreakServiceException) {
            assertThat(exception).isInstanceOf(CloudbreakServiceException.class);
            assertThat(exception).hasMessage(messageCloudbreakServiceExceptionExpected);
            assertThat(exception.getCause()).isSameAs(exceptionExpected);
        } else {
            assertThat(exception).isSameAs(exceptionExpected);
        }
    }

}