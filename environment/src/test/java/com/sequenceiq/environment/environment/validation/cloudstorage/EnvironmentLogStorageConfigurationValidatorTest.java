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
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

@ExtendWith(MockitoExtension.class)
public class EnvironmentLogStorageConfigurationValidatorTest {

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentTelemetry telemetry;

    @Mock
    private EnvironmentLogging logging;

    @InjectMocks
    private EnvironmentLogStorageConfigurationValidator underTest;

    @Test
    public void validateTelemetryStorageConfigNoBackup() {
        when(environment.getTelemetry()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigS3WhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile("arn:aws:iam::1234567:instance-profile/test");
        when(logging.getS3()).thenReturn(s3);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigS3WhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AWS);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile("arn:aws:iam::1234567:instance1-profile/test");
        when(logging.getS3()).thenReturn(s3);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertTrue(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigAzureWhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AZURE);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("/subscriptions/12345678-1234-0234-8234-123456789123/resourceGroups/test/" +
                "providers/Microsoft.ManagedIdentity/userAssignedIdentities/cdp-assumer");
        when(logging.getAdlsGen2()).thenReturn(adlsGen2);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigAzureWhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.AZURE);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("subscriptions1/a9d4sdfsdfsdf/resource1Groups/test/" +
                "providers/Microsoft.Network/networkSecurityGroups/default");
        when(logging.getAdlsGen2()).thenReturn(adlsGen2);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertTrue(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigGcpWhenConfigValidationSuccess() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.GCP);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        GcsCloudStorageV1Parameters gcp = new GcsCloudStorageV1Parameters();
        gcp.setServiceAccountEmail("admin1@dev-cdp.iam.gserviceaccount.com");
        when(logging.getGcs()).thenReturn(gcp);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryStorageConfigGcpWhenConfigValidationFailed() {
        when(environment.getCloudPlatform()).thenReturn(CloudConstants.GCP);
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        GcsCloudStorageV1Parameters gcp = new GcsCloudStorageV1Parameters();
        gcp.setServiceAccountEmail("admin1@dev-cdp.iam.gservice-account.com");
        when(logging.getGcs()).thenReturn(gcp);
        ValidationResult result = underTest.validateTelemetryLoggingStorageConfiguration(environment);
        assertTrue(result.hasError());
    }
}
