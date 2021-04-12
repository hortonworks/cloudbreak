package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBackupStorageConfigurationValidatorTest {

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentBackup backup;

    @InjectMocks
    private EnvironmentBackupStorageConfigurationValidator underTest;

    @Test
    public void validateBackupStorageConfigNoBackup() {
        when(environment.getBackup()).thenReturn(null);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigS3WhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        when(environment.getBackup()).thenReturn(backup);
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile("arn:aws:iam::1234567:instance-profile/test");
        when(backup.getS3()).thenReturn(s3);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigS3WhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        when(environment.getBackup()).thenReturn(backup);
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile("arn:aws:iam::1234567:instance1-profile/test");
        when(backup.getS3()).thenReturn(s3);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertTrue(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigAzureWhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AZURE);
        when(environment.getBackup()).thenReturn(backup);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("/subscriptions/12345678-1234-0234-8234-123456789123/resourceGroups/test/" +
                "providers/Microsoft.ManagedIdentity/userAssignedIdentities/cdp-assumer");
        when(backup.getAdlsGen2()).thenReturn(adlsGen2);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigAzureWhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AZURE);
        when(environment.getBackup()).thenReturn(backup);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("subscriptions1/a9d4sdfsdfsdf/resourceGroups/test/" +
                "providers/Microsoft.Network/networkSecurityGroups/default");
        when(backup.getAdlsGen2()).thenReturn(adlsGen2);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertTrue(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigGcpWhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.GCP);
        when(environment.getBackup()).thenReturn(backup);
        GcsCloudStorageV1Parameters gcp = new GcsCloudStorageV1Parameters();
        gcp.setServiceAccountEmail("admin1@dev-cdp.iam.gserviceaccount.com");
        when(backup.getGcs()).thenReturn(gcp);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateBackupStorageConfigGcpWhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.GCP);
        when(environment.getBackup()).thenReturn(backup);
        GcsCloudStorageV1Parameters gcp = new GcsCloudStorageV1Parameters();
        gcp.setServiceAccountEmail("admin1@dev-cdp.iam.gservice-account.com");
        when(backup.getGcs()).thenReturn(gcp);
        ValidationResult result = underTest.validateBackupStorageConfiguration(environment);
        assertTrue(result.hasError());
    }
}
