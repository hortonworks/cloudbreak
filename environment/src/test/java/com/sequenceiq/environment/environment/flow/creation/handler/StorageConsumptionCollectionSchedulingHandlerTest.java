package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionSchedulingHandlerTest {

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StorageConsumptionCollectionSchedulingHandler underTest;

    @Mock
    private Event<EnvironmentDto> environmentDtoEvent;

    private EnvironmentDto environmentDto;

    @Mock
    private Event.Headers headers;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEventCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersCaptor;

    @BeforeEach
    void setUp() {
        environmentDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .build();
        lenient().when(environmentDtoEvent.getData()).thenReturn(environmentDto);
        lenient().when(environmentDtoEvent.getHeaders()).thenReturn(headers);
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("SCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT");
    }

    @Test
    void acceptTestErrorWhenEnvironmentAbsent() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verifyFailureEvent(IllegalStateException.class, "Environment was not found with id '12'.");
        verify(eventSender, never()).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
    }

    private <E extends Exception> void verifyFailureEvent(Class<E> exceptionClassExpected, String exceptionMessageExpected) {
        verify(eventBus).notify(eq("FAILED_ENV_CREATION_EVENT"), eventCaptor.capture());

        Event<?> event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.getHeaders()).isSameAs(headers);

        Object eventData = event.getData();
        assertThat(eventData).isInstanceOf(EnvCreationFailureEvent.class);

        EnvCreationFailureEvent failureEvent = (EnvCreationFailureEvent) eventData;
        assertThat(failureEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(failureEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(failureEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);

        Exception failureEventException = failureEvent.getException();
        assertThat(failureEventException).isInstanceOf(exceptionClassExpected);
        assertThat(failureEventException).hasMessage(exceptionMessageExpected);
    }

    @Test
    void acceptTestErrorWhenException() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(new UnsupportedOperationException("This is not supported"));

        underTest.accept(environmentDtoEvent);

        verifyFailureEvent(UnsupportedOperationException.class, "This is not supported");
        verify(eventSender, never()).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
    }

    @Test
    void acceptTestSuccess() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(eventBus, never()).notify(any(), any(Event.class));
    }

    private void verifySuccessEvent() {
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersCaptor.capture());

        BaseNamedFlowEvent baseNamedFlowEvent = baseNamedFlowEventCaptor.getValue();
        assertThat(baseNamedFlowEvent).isInstanceOf(EnvCreationEvent.class);

        EnvCreationEvent successEvent = (EnvCreationEvent) baseNamedFlowEvent;
        assertThat(successEvent.selector()).isEqualTo("START_NETWORK_CREATION_EVENT");
        assertThat(successEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(successEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(successEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);

        assertThat(headersCaptor.getValue()).isSameAs(headers);
    }

}