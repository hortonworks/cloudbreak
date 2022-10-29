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
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentClientServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final String LOG_LOCATION = "s3a://path/to/logs";

    private static final String CLOUD_PLATFORM = "Azure";

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @InjectMocks
    private EnvironmentClientService underTest;

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
        Assert.assertEquals(BACKUP_LOCATION, underTest.getBackupLocation(ENV_CRN, false));
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
        Assert.assertEquals(LOG_LOCATION, underTest.getBackupLocation(ENV_CRN, false));
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
        underTest.getBackupLocation(ENV_CRN, false);
    }

    @Test
    public void testAzureRAZBackupLocation() {
        final String locationRootDir = "abfs://storagefs@mydatalake.dfs.core.windows.net";
        final String locationRootDirEndsWithSlash = "abfs://storagefs@mydatalake.dfs.core.windows.net/";
        final String locationSubDir = "abfs://storagefs@mydatalake.dfs.core.windows.net/data";
        final String locationSubDirEndsWithSlash = "abfs://storagefs@mydatalake.dfs.core.windows.net/data/";
        final String locationSubDirWithMultipleSlashes = "abfs://storagefs@mydatalake.dfs.core.windows.net/data/data2";
        final String locationSubDirWithMultipleSlashesEndsWithSlash = "abfs://storagefs@mydatalake.dfs.core.windows.net/data/data2/data3/";
        final String locationNotStartsWithABFS = "abffs://storagefs@mydatalake.dfs.core.windows.net";

        BackupResponse backupResponse = new BackupResponse();
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();

        //Set up an AZURE environment with root directory backup location.
        environmentResponse.setCrn(ENV_CRN);
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM);
        environmentResponse.setBackup(backupResponse);
        environmentResponse.getBackup().setStorageLocation(locationRootDir);
        environmentResponse.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(AzureResourceGroup.builder()
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build())
                .build());
        when(environmentEndpoint.getByCrn(anyString())).thenReturn(environmentResponse);

        Assert.assertEquals(locationRootDir + "/backups", underTest.getBackupLocation(ENV_CRN, true));

        environmentResponse.getBackup().setStorageLocation(locationRootDirEndsWithSlash);
        Assert.assertEquals(locationRootDirEndsWithSlash +"backups", underTest.getBackupLocation(ENV_CRN, true));

        //Non-RAZ will not be appended with "backups" or "/backups".
        Assert.assertEquals(locationRootDirEndsWithSlash, underTest.getBackupLocation(ENV_CRN, false));

        //Set up an AZURE environment with subdirectory backup location, which will not be appended with "backups" or "/backups".
        environmentResponse.getBackup().setStorageLocation(locationSubDir);
        Assert.assertEquals(locationSubDir, underTest.getBackupLocation(ENV_CRN, true));

        environmentResponse.getBackup().setStorageLocation(locationSubDirEndsWithSlash);
        Assert.assertEquals(locationSubDirEndsWithSlash, underTest.getBackupLocation(ENV_CRN, true));

        environmentResponse.getBackup().setStorageLocation(locationSubDirWithMultipleSlashes);
        Assert.assertEquals(locationSubDirWithMultipleSlashes, underTest.getBackupLocation(ENV_CRN, true));

        environmentResponse.getBackup().setStorageLocation(locationSubDirWithMultipleSlashesEndsWithSlash);
        Assert.assertEquals(locationSubDirWithMultipleSlashesEndsWithSlash, underTest.getBackupLocation(ENV_CRN, true));

        //Use a location that does not start with "abfs://", which will return the original path.
        environmentResponse.getBackup().setStorageLocation(locationNotStartsWithABFS);
        Assert.assertEquals(locationNotStartsWithABFS, underTest.getBackupLocation(ENV_CRN, true));

        //Set up an AWS environment with root directory backup location, which will not be appended with "backups" or "/backups".
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.getBackup().setStorageLocation(locationRootDir);
        Assert.assertEquals(locationRootDir, underTest.getBackupLocation(ENV_CRN, true));

    }

}
