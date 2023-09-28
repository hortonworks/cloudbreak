package com.sequenceiq.environment.environment.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;

@ExtendWith(MockitoExtension.class)
class BackupConverterTest {

    private static final String STORAGE_LOCATION = "storageLocation";

    @Mock
    private StorageLocationDecorator storageLocationDecorator;

    @InjectMocks
    private BackupConverter underTest;

    @Test
    void convertTestBackupRequestWhenNull() {
        EnvironmentBackup result = underTest.convert((BackupRequest) null);

        assertThat(result).isNull();
    }

    @Test
    void convertTestBackupRequestWhenBackupStorageLocation() {
        BackupRequest request = new BackupRequest();
        request.setStorageLocation(STORAGE_LOCATION);

        EnvironmentBackup result = underTest.convert(request);

        assertThat(result).isNotNull();
        verify(storageLocationDecorator).setBackupStorageLocationFromRequest(result, STORAGE_LOCATION);
    }

    @Test
    void convertTestTelemetryRequestWhenNullTelemetry() {
        EnvironmentBackup result = underTest.convert((TelemetryRequest) null);

        assertThat(result).isNull();
    }

    @Test
    void convertTestTelemetryRequestWhenNullLogging() {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        telemetryRequest.setLogging(null);

        EnvironmentBackup result = underTest.convert(telemetryRequest);

        assertThat(result).isNull();
    }

    @Test
    void convertTestTelemetryRequestWhenBackupStorageLocation() {
        TelemetryRequest telemetryRequest = new TelemetryRequest();
        LoggingRequest loggingRequest = new LoggingRequest();
        telemetryRequest.setLogging(loggingRequest);
        loggingRequest.setStorageLocation(STORAGE_LOCATION);

        EnvironmentBackup result = underTest.convert(telemetryRequest);

        assertThat(result).isNotNull();
        verify(storageLocationDecorator).setBackupStorageLocationFromRequest(result, STORAGE_LOCATION);
    }

}