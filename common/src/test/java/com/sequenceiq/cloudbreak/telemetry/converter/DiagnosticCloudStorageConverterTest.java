package com.sequenceiq.cloudbreak.telemetry.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.GcsConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.S3ConfigGenerator;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.model.diagnostics.AwsDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.AzureDiagnosticParameters;
import com.sequenceiq.common.model.diagnostics.CloudStorageDiagnosticsParameters;
import com.sequenceiq.common.model.diagnostics.GcsDiagnosticsParameters;

@ExtendWith(MockitoExtension.class)
class DiagnosticCloudStorageConverterTest {

    private static final String STORAGE_LOCATION = "location";

    private static final String REGION = "region";

    private static final String FOLDER_PREFIX = "prefix";

    private static final String BUCKET = "bucket";

    private static final String FILE_SYSTEM = "fileSystem";

    private static final String ACCOUNT = "account";

    private static final String LOCATION = "prefix/diagnostics";

    private static final String PROJECT_ID = "projectId";

    @Mock
    private S3ConfigGenerator s3ConfigGenerator;

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Mock
    private GcsConfigGenerator gcsConfigGenerator;

    @InjectMocks
    private DiagnosticCloudStorageConverter underTest;

    @Test
    void testLoggingToCloudStorageDiagnosticsParametersWhenLoggingIsNull() {
        CloudStorageDiagnosticsParameters result = underTest.loggingToCloudStorageDiagnosticsParameters(null, REGION);
        assertNull(result);
    }

    @Test
    void testLoggingResponseToCloudStorageDiagnosticsParametersWhenLoggingIsNull() {
        CloudStorageDiagnosticsParameters result = underTest.loggingResponseToCloudStorageDiagnosticsParameters(null, REGION);
        assertNull(result);
    }

    @Test
    void testLoggingToCloudStorageDiagnosticsParametersWhenLoggingIsEmpty() {
        CloudStorageDiagnosticsParameters result = underTest.loggingToCloudStorageDiagnosticsParameters(new Logging(), REGION);
        assertNull(result);
    }

    @Test
    void testLoggingResponseToCloudStorageDiagnosticsParametersWhenLoggingIsEmpty() {
        CloudStorageDiagnosticsParameters result = underTest.loggingResponseToCloudStorageDiagnosticsParameters(new LoggingResponse(), REGION);
        assertNull(result);
    }

    @Test
    void testLoggingToCloudStorageDiagnosticsParametersWhenLoggingS3() {
        when(s3ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new S3Config(FOLDER_PREFIX, BUCKET));
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setS3(new S3CloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingToCloudStorageDiagnosticsParameters(logging, REGION);
        assertAwsDiagnosticParameters(result);
    }

    @Test
    void testLoggingResponseToCloudStorageDiagnosticsParametersWhenLoggingS3() {
        when(s3ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new S3Config(FOLDER_PREFIX, BUCKET));
        LoggingResponse logging = new LoggingResponse();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setS3(new S3CloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingResponseToCloudStorageDiagnosticsParameters(logging, REGION);
        assertAwsDiagnosticParameters(result);
    }

    @Test
    void testLoggingToCloudStorageDiagnosticsParametersWhenLoggingAdlsGen2() {
        when(adlsGen2ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new AdlsGen2Config(FOLDER_PREFIX, FILE_SYSTEM, ACCOUNT, true));
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingToCloudStorageDiagnosticsParameters(logging, REGION);
        assertAzureDiagnosticParameters(result);
    }

    @Test
    void testLoggingResponseToCloudStorageDiagnosticsParametersWhenLoggingAdlsGen2() {
        when(adlsGen2ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new AdlsGen2Config(FOLDER_PREFIX, FILE_SYSTEM, ACCOUNT, true));
        LoggingResponse logging = new LoggingResponse();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingResponseToCloudStorageDiagnosticsParameters(logging, REGION);
        assertAzureDiagnosticParameters(result);
    }

    @Test
    void testLoggingToCloudStorageDiagnosticsParametersWhenLoggingGcs() {
        when(gcsConfigGenerator.generateStorageConfig(anyString())).thenReturn(new GcsConfig(FOLDER_PREFIX, BUCKET, PROJECT_ID));
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setGcs(new GcsCloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingToCloudStorageDiagnosticsParameters(logging, REGION);
        assertGcsDiagnosticParameters(result);
    }

    @Test
    void testLoggingResponseToCloudStorageDiagnosticsParametersWhenLoggingGcs() {
        when(gcsConfigGenerator.generateStorageConfig(anyString())).thenReturn(new GcsConfig(FOLDER_PREFIX, BUCKET, PROJECT_ID));
        LoggingResponse logging = new LoggingResponse();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setGcs(new GcsCloudStorageV1Parameters());
        CloudStorageDiagnosticsParameters result = underTest.loggingResponseToCloudStorageDiagnosticsParameters(logging, REGION);
        assertGcsDiagnosticParameters(result);
    }

    private void assertAwsDiagnosticParameters(CloudStorageDiagnosticsParameters result) {
        assertNotNull(result);
        verify(s3ConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        assertThat(result).isInstanceOf(AwsDiagnosticParameters.class);
        AwsDiagnosticParameters awsDiagnosticParameters = (AwsDiagnosticParameters) result;
        assertEquals(BUCKET, awsDiagnosticParameters.getS3Bucket());
        assertEquals(LOCATION, awsDiagnosticParameters.getS3Location());
        assertEquals(REGION, awsDiagnosticParameters.getS3Region());
    }

    private void assertAzureDiagnosticParameters(CloudStorageDiagnosticsParameters result) {
        assertNotNull(result);
        verify(s3ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        assertThat(result).isInstanceOf(AzureDiagnosticParameters.class);
        AzureDiagnosticParameters azureDiagnosticParameters = (AzureDiagnosticParameters) result;
        assertEquals(FILE_SYSTEM, azureDiagnosticParameters.getAdlsv2StorageContainer());
        assertEquals(LOCATION, azureDiagnosticParameters.getAdlsv2StorageLocation());
        assertEquals(ACCOUNT, azureDiagnosticParameters.getAdlsv2StorageAccount());
    }

    private void assertGcsDiagnosticParameters(CloudStorageDiagnosticsParameters result) {
        assertNotNull(result);
        verify(s3ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        assertThat(result).isInstanceOf(GcsDiagnosticsParameters.class);
        GcsDiagnosticsParameters gcsDiagnosticParameters = (GcsDiagnosticsParameters) result;
        assertEquals(BUCKET, gcsDiagnosticParameters.getBucket());
        assertEquals(LOCATION, gcsDiagnosticParameters.getGcsLocation());
    }
}