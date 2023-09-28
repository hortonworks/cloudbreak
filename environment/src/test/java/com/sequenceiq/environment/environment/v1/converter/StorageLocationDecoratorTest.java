package com.sequenceiq.environment.environment.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;

class StorageLocationDecoratorTest {

    private static final String STORAGE_LOCATION_WITH_PROTOCOL = "foo://bar";

    private static final String STORAGE_LOCATION_WITHOUT_PROTOCOL = "baz";

    private static final String PROTOCOL_S3A = "s3a://";

    private static final String PROTOCOL_ABFS = "abfs://";

    private static final String PROTOCOL_GS = "gs://";

    private StorageLocationDecorator underTest;

    @BeforeEach
    void setUp() {
        underTest = new StorageLocationDecorator();
    }

    @ParameterizedTest(name = "storageLocation='{0}'")
    @ValueSource(strings = {"", " "})
    @NullSource
    void setBackupStorageLocationFromRequestTestWhenBlankStorageLocation(String storageLocation) {
        EnvironmentBackup backup = new EnvironmentBackup();

        underTest.setBackupStorageLocationFromRequest(backup, storageLocation);

        assertThat(backup.getStorageLocation()).isEqualTo(storageLocation);
    }

    @Test
    void setBackupStorageLocationFromRequestTestWhenStorageLocationWithProtocol() {
        EnvironmentBackup backup = new EnvironmentBackup();

        underTest.setBackupStorageLocationFromRequest(backup, STORAGE_LOCATION_WITH_PROTOCOL);

        assertThat(backup.getStorageLocation()).isEqualTo(STORAGE_LOCATION_WITH_PROTOCOL);
    }

    @Test
    void setBackupStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndUnknownFileSystem() {
        EnvironmentBackup backup = new EnvironmentBackup();

        underTest.setBackupStorageLocationFromRequest(backup, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(backup.getStorageLocation()).isEqualTo(STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setBackupStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndS3() {
        EnvironmentBackup backup = new EnvironmentBackup();
        backup.setS3(new S3CloudStorageParameters());

        underTest.setBackupStorageLocationFromRequest(backup, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(backup.getStorageLocation()).isEqualTo(PROTOCOL_S3A + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setBackupStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndAdlsGen2() {
        EnvironmentBackup backup = new EnvironmentBackup();
        backup.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());

        underTest.setBackupStorageLocationFromRequest(backup, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(backup.getStorageLocation()).isEqualTo(PROTOCOL_ABFS + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setBackupStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndGcs() {
        EnvironmentBackup backup = new EnvironmentBackup();
        backup.setGcs(new GcsCloudStorageV1Parameters());

        underTest.setBackupStorageLocationFromRequest(backup, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(backup.getStorageLocation()).isEqualTo(PROTOCOL_GS + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @ParameterizedTest(name = "storageLocation='{0}'")
    @ValueSource(strings = {"", " "})
    @NullSource
    void setLoggingStorageLocationFromRequestTestWhenBlankStorageLocation(String storageLocation) {
        EnvironmentLogging logging = new EnvironmentLogging();

        underTest.setLoggingStorageLocationFromRequest(logging, storageLocation);

        assertThat(logging.getStorageLocation()).isEqualTo(storageLocation);
    }

    @Test
    void setLoggingStorageLocationFromRequestTestWhenStorageLocationWithProtocol() {
        EnvironmentLogging logging = new EnvironmentLogging();

        underTest.setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION_WITH_PROTOCOL);

        assertThat(logging.getStorageLocation()).isEqualTo(STORAGE_LOCATION_WITH_PROTOCOL);
    }

    @Test
    void setLoggingStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndUnknownFileSystem() {
        EnvironmentLogging logging = new EnvironmentLogging();

        underTest.setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(logging.getStorageLocation()).isEqualTo(STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setLoggingStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndS3() {
        EnvironmentLogging logging = new EnvironmentLogging();
        logging.setS3(new S3CloudStorageParameters());

        underTest.setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(logging.getStorageLocation()).isEqualTo(PROTOCOL_S3A + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setLoggingStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndAdlsGen2() {
        EnvironmentLogging logging = new EnvironmentLogging();
        logging.setAdlsGen2(new AdlsGen2CloudStorageV1Parameters());

        underTest.setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(logging.getStorageLocation()).isEqualTo(PROTOCOL_ABFS + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

    @Test
    void setLoggingStorageLocationFromRequestTestWhenStorageLocationWithoutProtocolAndGcs() {
        EnvironmentLogging logging = new EnvironmentLogging();
        logging.setGcs(new GcsCloudStorageV1Parameters());

        underTest.setLoggingStorageLocationFromRequest(logging, STORAGE_LOCATION_WITHOUT_PROTOCOL);

        assertThat(logging.getStorageLocation()).isEqualTo(PROTOCOL_GS + STORAGE_LOCATION_WITHOUT_PROTOCOL);
    }

}