package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@ExtendWith(MockitoExtension.class)
public class EnvironmentLogStorageLocationValidatorTest {

    private static final String REGION_1 = "region-1";

    @Mock
    private CloudStorageLocationValidator validator;

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentTelemetry telemetry;

    @Mock
    private EnvironmentLogging logging;

    @InjectMocks
    private EnvironmentLogStorageLocationValidator underTest;

    @Test
    public void validateTelemetryLoggingStorageLocationNoTelemetry() {
        when(environment.getTelemetry()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationNoLogging() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationNoStoragerLocation() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationValidatorPasses() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(REGION_1);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertFalse(result.hasError());
    }
}
