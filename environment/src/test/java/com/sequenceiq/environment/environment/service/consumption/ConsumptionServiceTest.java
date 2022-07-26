package com.sequenceiq.environment.environment.service.consumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

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
    private EntitlementService entitlementService;

    @Mock
    private ConsumptionClientService consumptionClientService;

    private ConsumptionService underTest;

    private EnvironmentDto environmentDto;

    @Captor
    private ArgumentCaptor<StorageConsumptionRequest> storageConsumptionRequestCaptor;

    @BeforeEach
    void setUp() {
        underTest = new ConsumptionService(entitlementService, consumptionClientService, CONSUMPTION_ENABLED);

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
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenDeploymentFlagDisabled() {
        underTest = new ConsumptionService(entitlementService, consumptionClientService, CONSUMPTION_DISABLED);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoTelemetryAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);
        environmentDto.setBackup(null);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoLoggingAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);
        environmentDto.setBackup(null);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @ParameterizedTest(name = "storageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoStorageLocations(String storageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(storageLocation);
        environmentDto.getBackup().setStorageLocation(storageLocation);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class));
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenBothLocationsProvidedAndDifferent() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, times(2)).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
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
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenBothLocationsProvidedAndSameSoBackupIsSkipped() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(LOGGING_STORAGE_LOCATION);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyLoggingLocationProvidedAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setBackup(null);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "backupStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyLoggingLocationProvided(String backupStorageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(backupStorageLocation);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), LOGGING_STORAGE_LOCATION);
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvidedAndNoTelemetry() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvidedAndNoLogging() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "loggingStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void scheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvided(String loggingStorageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(loggingStorageLocation);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue(), BACKUP_STORAGE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenDeploymentFlagDisabled() {
        underTest = new ConsumptionService(entitlementService, consumptionClientService, CONSUMPTION_DISABLED);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoTelemetryAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);
        environmentDto.setBackup(null);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoLoggingAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);
        environmentDto.setBackup(null);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "storageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoStorageLocations(String storageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(storageLocation);
        environmentDto.getBackup().setStorageLocation(storageLocation);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString());
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenBothLocationsProvidedAndDifferent() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenBothLocationsProvidedAndSameSoBackupIsSkipped() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(LOGGING_STORAGE_LOCATION);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyLoggingLocationProvidedAndNoBackup() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setBackup(null);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "backupStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyLoggingLocationProvided(String backupStorageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getBackup().setStorageLocation(backupStorageLocation);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, LOGGING_STORAGE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvidedAndNoTelemetry() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.setTelemetry(null);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvidedAndNoLogging() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().setLogging(null);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

    @ParameterizedTest(name = "loggingStorageLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_LOCATION})
    @NullSource
    void unscheduleStorageConsumptionCollectionIfNeededTestSuccessWhenOnlyBackupLocationProvided(String loggingStorageLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);
        environmentDto.getTelemetry().getLogging().setStorageLocation(loggingStorageLocation);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(environmentDto);

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, ENVIRONMENT_CRN, BACKUP_STORAGE_LOCATION);
    }

}