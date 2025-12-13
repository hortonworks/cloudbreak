package com.sequenceiq.datalake.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final String LOG_LOCATION = "s3a://path/to/logs";

    private static final String CLOUD_PLATFORM = "Azure";

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    void testSdxBackupLocationOnUpgradeRequestEnabled() {
        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setStorageLocation(BACKUP_LOCATION);
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setStorageLocation(LOG_LOCATION);
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        telemetryResponse.setLogging(loggingResponse);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setBackup(backupResponse);
        environmentResponse.setTelemetry(telemetryResponse);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());

        when(environmentEndpoint.getByCrn(anyString())).thenReturn(environmentResponse);
        assertEquals(BACKUP_LOCATION, underTest.getBackupLocation(ENV_CRN));
    }

    @Test
    void testSdxLogLocationOnUpgradeRequestEnabled() {
        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setStorageLocation(LOG_LOCATION);
        TelemetryResponse telemetryResponse = new TelemetryResponse();
        telemetryResponse.setLogging(loggingResponse);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setTelemetry(telemetryResponse);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());

        when(environmentEndpoint.getByCrn(anyString())).thenReturn(environmentResponse);
        assertEquals(LOG_LOCATION, underTest.getBackupLocation(ENV_CRN));
    }

    @Test
    void testSdxNoLocationOnUpgradeRequestEnabled() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());

        when(environmentEndpoint.getByCrn(anyString())).thenReturn(environmentResponse);
        assertThrows(BadRequestException.class, () -> underTest.getBackupLocation(ENV_CRN));
    }

}
