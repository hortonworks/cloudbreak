package com.sequenceiq.datalake.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final String LOG_LOCATION = "s3a://path/to/logs";

    private static final String CLOUD_PLATFORM = "Azure";

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    public void testSdxBackupLocationOnUpgradeRequestEnabled() {
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
        Assert.assertEquals(BACKUP_LOCATION, underTest.getBackupLocation(ENV_CRN));
    }

    @Test
    public void testSdxLogLocationOnUpgradeRequestEnabled() {
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
        Assert.assertEquals(LOG_LOCATION, underTest.getBackupLocation(ENV_CRN));
    }

    @Test(expected = BadRequestException.class)
    public void testSdxNoLocationOnUpgradeRequestEnabled() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());

        when(environmentEndpoint.getByCrn(anyString())).thenReturn(environmentResponse);
        underTest.getBackupLocation(ENV_CRN);
    }

}
