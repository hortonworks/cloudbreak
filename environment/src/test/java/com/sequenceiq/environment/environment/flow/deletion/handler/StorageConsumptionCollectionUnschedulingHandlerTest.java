package com.sequenceiq.environment.environment.flow.deletion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.consumption.ConsumptionService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionUnschedulingHandlerTest {

    private static final String SELECTOR = "UNSCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT";

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HandlerExceptionProcessor exceptionProcessor;

    @Mock
    private ConsumptionService consumptionService;

    @InjectMocks
    private StorageConsumptionCollectionUnschedulingHandler underTest;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDeletionDtoEvent;

    private EnvironmentDto environmentDto;

    @Mock
    private Event.Headers headers;

    @Captor
    private ArgumentCaptor<HandlerFailureConjoiner> handlerFailureConjoinerCaptor;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEventCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersCaptor;

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo(SELECTOR);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestErrorWhenException(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        UnsupportedOperationException exception = new UnsupportedOperationException("This is not supported");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(exception);

        underTest.accept(environmentDeletionDtoEvent);

        verify(consumptionService, never()).unscheduleStorageConsumptionCollectionIfNeeded(any(EnvironmentDto.class));
        verify(eventSender, never()).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
        verify(exceptionProcessor).handle(handlerFailureConjoinerCaptor.capture(), any(Logger.class), eq(eventSender), eq(SELECTOR));

        HandlerFailureConjoiner handlerFailureConjoiner = handlerFailureConjoinerCaptor.getValue();
        assertThat(handlerFailureConjoiner).isNotNull();
        assertThat(handlerFailureConjoiner.getException()).isSameAs(exception);
        assertThat(handlerFailureConjoiner.getEnvironmentDtoEvent()).isSameAs(environmentDeletionDtoEvent);
        assertThat(handlerFailureConjoiner.getEnvironmentDeletionDto()).isSameAs(environmentDeletionDtoEvent.getData());
        assertThat(handlerFailureConjoiner.getEnvironmentDto()).isSameAs(environmentDeletionDtoEvent.getData().getEnvironmentDto());
        verifyNextStateEvent(handlerFailureConjoiner.getEnvDeleteEvent(), forceDelete);
    }

    private void initEnvironmentDeletionDtoEvent(boolean forceDelete) {
        environmentDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .build();
        EnvironmentDeletionDto environmentDeletionDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withId(ENVIRONMENT_ID)
                .withForceDelete(forceDelete)
                .build();
        when(environmentDeletionDtoEvent.getData()).thenReturn(environmentDeletionDto);
        lenient().when(environmentDeletionDtoEvent.getHeaders()).thenReturn(headers);
    }

    private void verifyNextStateEvent(EnvDeleteEvent nextStateEvent, boolean forceDelete) {
        assertThat(nextStateEvent.selector()).isEqualTo("START_RDBMS_DELETE_EVENT");
        assertThat(nextStateEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(nextStateEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(nextStateEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(nextStateEvent.isForceDelete()).isEqualTo(forceDelete);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSkipWhenEnvironmentAbsent(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollectionIfNeeded(any(EnvironmentDto.class));
        verify(exceptionProcessor, never()).handle(any(HandlerFailureConjoiner.class), any(Logger.class), any(EventSender.class), anyString());
    }

    private void verifySuccessEvent(boolean forceDelete) {
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersCaptor.capture());

        BaseNamedFlowEvent baseNamedFlowEvent = baseNamedFlowEventCaptor.getValue();
        assertThat(baseNamedFlowEvent).isInstanceOf(EnvDeleteEvent.class);
        verifyNextStateEvent((EnvDeleteEvent) baseNamedFlowEvent, forceDelete);

        assertThat(headersCaptor.getValue()).isSameAs(headers);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSuccess(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);
        verify(exceptionProcessor, never()).handle(any(HandlerFailureConjoiner.class), any(Logger.class), any(EventSender.class), anyString());
    }

}