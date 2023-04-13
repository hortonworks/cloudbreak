package com.sequenceiq.cloudbreak.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.common.api.diagnostics.BaseCmDiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

@ExtendWith(MockitoExtension.class)
class CmDiagnosticsDataToParameterConverterTest {

    private static final String STORAGE_LOCATION = "location";

    private static final String LOCATION = "prefix/diagnostics";

    private static final String REGION = "region";

    private static final String FOLDER_PREFIX = "prefix";

    private static final String BUCKET = "bucket";

    private static final String FILE_SYSTEM = "fileSystem";

    private static final String ACCOUNT = "account";

    private static final String PROJECT_ID = "projectId";

    @Mock
    private S3ConfigGenerator s3ConfigGenerator;

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Mock
    private GcsConfigGenerator gcsConfigGenerator;

    @InjectMocks
    private CmDiagnosticsDataToParameterConverter underTest;

    @Test
    void testConvertWhenLoggingIsNull() {
        BaseCmDiagnosticsCollectionRequest request = mock(BaseCmDiagnosticsCollectionRequest.class);
        CmDiagnosticsParameters result = underTest.convert(request, new Telemetry(), "clusterName", "region");
        assertNotNull(result);
        assertNull(result.getS3Location());
        assertNull(result.getAdlsv2StorageLocation());
        assertNull(result.getGcsLocation());
    }

    @Test
    void testConvertWhenLoggingS3() {
        when(s3ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new S3Config(FOLDER_PREFIX, BUCKET));
        BaseCmDiagnosticsCollectionRequest request = mock(BaseCmDiagnosticsCollectionRequest.class);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setS3(new S3CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        CmDiagnosticsParameters result = underTest.convert(request, telemetry, "clusterName", REGION);
        verify(s3ConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        assertNotNull(result);
        assertEquals(LOCATION, result.getS3Location());
        assertEquals(BUCKET, result.getS3Bucket());
        assertEquals(REGION, result.getS3Region());
    }

    @Test
    void testConvertWhenLoggingAdlsGen2() {
        when(adlsGen2ConfigGenerator.generateStorageConfig(anyString())).thenReturn(new AdlsGen2Config(FOLDER_PREFIX, FILE_SYSTEM, ACCOUNT, true));
        BaseCmDiagnosticsCollectionRequest request = mock(BaseCmDiagnosticsCollectionRequest.class);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());
        telemetry.setLogging(logging);
        CmDiagnosticsParameters result = underTest.convert(request, telemetry, "clusterName", REGION);
        verify(s3ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        assertNotNull(result);
        assertEquals(LOCATION, result.getAdlsv2StorageLocation());
        assertEquals(FILE_SYSTEM, result.getAdlsv2StorageContainer());
        assertEquals(ACCOUNT, result.getAdlsv2StorageAccount());
    }

    @Test
    void testConvertWhenLoggingGcs() {
        when(gcsConfigGenerator.generateStorageConfig(anyString())).thenReturn(new GcsConfig(FOLDER_PREFIX, BUCKET, PROJECT_ID));
        BaseCmDiagnosticsCollectionRequest request = mock(BaseCmDiagnosticsCollectionRequest.class);
        Telemetry telemetry = new Telemetry();
        Logging logging = new Logging();
        logging.setStorageLocation(STORAGE_LOCATION);
        logging.setGcs(new GcsCloudStorageV1Parameters());
        telemetry.setLogging(logging);
        CmDiagnosticsParameters result = underTest.convert(request, telemetry, "clusterName", REGION);
        verify(s3ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(adlsGen2ConfigGenerator, never()).generateStorageConfig(eq(STORAGE_LOCATION));
        verify(gcsConfigGenerator, times(1)).generateStorageConfig(eq(STORAGE_LOCATION));
        assertNotNull(result);
        assertEquals(LOCATION, result.getGcsLocation());
        assertEquals(BUCKET, result.getGcsBucket());
    }
}