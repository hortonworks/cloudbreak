package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.consumption.ConsumptionService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class StorageConsumptionCollectionSchedulingHandlerTest {

    private static final boolean CONSUMPTION_ENABLED = true;

    private static final boolean CONSUMPTION_DISABLED = false;

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String LOGGING_STORAGE_LOCATION = "s3a://foo/bar";

    private static final String BACKUP_STORAGE_LOCATION = "s3a://baz";

    private static final String EMPTY_STORAGE_LOCATION = "";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventBus eventBus;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ConsumptionService consumptionService;

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

    @Captor
    private ArgumentCaptor<StorageConsumptionRequest> storageConsumptionRequestCaptor;

    @BeforeEach
    void setUp() {
        underTest = new StorageConsumptionCollectionSchedulingHandler(eventSender, environmentService, eventBus, entitlementService, consumptionService,
                CONSUMPTION_ENABLED);

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
    }

    @Test
    void acceptTestSkipWhenDeploymentFlagDisabled() {
        underTest = new StorageConsumptionCollectionSchedulingHandler(eventSender, environmentService, eventBus, entitlementService, consumptionService,
                CONSUMPTION_DISABLED);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
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

    @Test
    void acceptTestSkipWhenEntitlementDisabled() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(consumptionService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void acceptTestSkipWhenNoTelemetryAndNoBackup() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);
        environmentDto.setBackup(null);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(consumptionService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void acceptTestSkipWhenNoLoggingAndNoBackup() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);
        environmentDto.setBackup(null);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(consumptionService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @ParameterizedTest(name = "storageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void acceptTestSkipWhenNoStorageLocations(String storageLocation) {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(storageLocation);
        environmentDto.getBackup().setStorageLocation(storageLocation);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();
        verify(consumptionService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void acceptTestSuccessWhenBothLocationsProvidedAndDifferent() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService, times(2)).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        List<StorageConsumptionRequest> storageConsumptionRequests = storageConsumptionRequestCaptor.getAllValues();
        assertThat(storageConsumptionRequests).hasSize(2);
        verifyStorageConsumptionRequest(storageConsumptionRequests.get(0), LOGGING_STORAGE_LOCATION);
        verifyStorageConsumptionRequest(storageConsumptionRequests.get(1), BACKUP_STORAGE_LOCATION);
    }

    private void verifyStorageConsumptionRequest(StorageConsumptionRequest storageConsumptionRequest, String storageLocation) {
        assertThat(storageConsumptionRequest).isNotNull();
        assertThat(storageConsumptionRequest.getEnvironmentCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(storageConsumptionRequest.getMonitoredResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(storageConsumptionRequest.getMonitoredResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(storageConsumptionRequest.getMonitoredResourceType()).isEqualTo(ResourceType.ENVIRONMENT);
        assertThat(storageConsumptionRequest.getStorageLocation()).isEqualTo(storageLocation);
    }

    @Test
    void acceptTestSuccessWhenBothLocationsProvidedAndSameSoBackupIsSkipped() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(LOGGING_STORAGE_LOCATION);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @Test
    void acceptTestSuccessWhenOnlyLoggingLocationProvidedAndNoBackup() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setBackup(null);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "backupStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void acceptTestSuccessWhenOnlyLoggingLocationProvided(String backupStorageLocation) {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(backupStorageLocation);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @Test
    void acceptTestSuccessWhenOnlyBackupLocationProvidedAndNoTelemetry() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

    @Test
    void acceptTestSuccessWhenOnlyBackupLocationProvidedAndNoLogging() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "loggingStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void acceptTestSuccessWhenOnlyBackupLocationProvided(String loggingStorageLocation) {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(new Environment()));
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(loggingStorageLocation);

        underTest.accept(environmentDtoEvent);

        verifySuccessEvent();

        verify(consumptionService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

}