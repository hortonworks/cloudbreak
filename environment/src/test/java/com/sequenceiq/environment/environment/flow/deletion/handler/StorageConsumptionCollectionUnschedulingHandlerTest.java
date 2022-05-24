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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.consumption.ConsumptionService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionUnschedulingHandlerTest {

    private static final boolean CONSUMPTION_ENABLED = true;

    private static final boolean CONSUMPTION_DISABLED = false;

    private static final String SELECTOR = "UNSCHEDULE_STORAGE_CONSUMPTION_COLLECTION_EVENT";

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String LOGGING_STORAGE_LOCATION = "s3a://foo/bar";

    private static final String BACKUP_STORAGE_LOCATION = "s3a://baz";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HandlerExceptionProcessor exceptionProcessor;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ConsumptionService consumptionService;

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

    @BeforeEach
    void setUp() {
        underTest = new StorageConsumptionCollectionUnschedulingHandler(eventSender, environmentService, exceptionProcessor, entitlementService,
                consumptionService, CONSUMPTION_ENABLED);
    }

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
        EnvironmentLogging environmentLogging = new EnvironmentLogging();
        environmentLogging.setStorageLocation(LOGGING_STORAGE_LOCATION);
        EnvironmentTelemetry environmentTelemetry = new EnvironmentTelemetry();
        environmentTelemetry.setLogging(environmentLogging);
        EnvironmentBackup backup = new EnvironmentBackup();
        backup.setStorageLocation(BACKUP_STORAGE_LOCATION);
        environmentDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withAccountId(ACCOUNT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .withTelemetry(environmentTelemetry)
                .withBackup(backup)
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
        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
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
    void acceptTestSkipWhenDeploymentFlagDisabled(boolean forceDelete) {
        underTest = new StorageConsumptionCollectionUnschedulingHandler(eventSender, environmentService, exceptionProcessor, entitlementService,
                consumptionService, CONSUMPTION_DISABLED);
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSkipWhenEntitlementDisabled(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSkipWhenNoTelemetryAndNoBackup(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);
        environmentDto.setBackup(null);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSkipWhenNoLoggingAndNoBackup(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);
        environmentDto.setBackup(null);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSkipWhenNoStorageLocations(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(null);
        environmentDto.getBackup().setStorageLocation(null);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSuccessWhenBothLocationsProvidedAndDifferent(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
        verify(consumptionService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSuccessWhenBothLocationsProvidedAndSameSoBackupIsSkipped(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(LOGGING_STORAGE_LOCATION);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSuccessWhenOnlyLoggingLocationProvided(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setBackup(null);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {false, true})
    void acceptTestSuccessWhenOnlyBackupLocationProvided(boolean forceDelete) {
        initEnvironmentDeletionDtoEvent(forceDelete);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);

        underTest.accept(environmentDeletionDtoEvent);

        verifySuccessEvent(forceDelete);
        verify(consumptionService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

}