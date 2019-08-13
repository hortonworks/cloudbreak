package com.sequenceiq.environment.environment.validation.cloudstorage;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentLogStorageLocationValidatorTest {

    private static final String REGION_1 = "region-1";

    private static final String USER_CRN = "userCrn";

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
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(USER_CRN, environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationNoLogging() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(USER_CRN, environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationNoStoragerLocation() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(USER_CRN, environment);
        assertFalse(result.hasError());
    }

    @Test
    public void validateTelemetryLoggingStorageLocationValidatorPasses() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(REGION_1);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(USER_CRN, environment);
        assertFalse(result.hasError());
    }
}
