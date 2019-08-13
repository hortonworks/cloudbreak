package com.sequenceiq.environment.environment.validation.storagelocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentLogStorageLocationValidatorTest {

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION_1 = "region-1";

    @Mock
    private Map<CloudPlatform, EnvironmentTelemetryLoggingStorageLocationValidator> loggingStorageLocationValidatorsByCloudPlatform;

    @Mock
    private EnvironmentTelemetryLoggingStorageLocationValidator validator;

    @Mock
    private Environment environment;

    @Mock
    private EnvironmentTelemetry telemetry;

    @Mock
    private EnvironmentLogging logging;

    @InjectMocks
    private EnvironmentLogStorageLocationValidator underTest;

    @Before
    public void setUp() {
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
    }

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
    public void validateTelemetryLoggingStorageLocationNoValidator() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(REGION_1);
        when(loggingStorageLocationValidatorsByCloudPlatform.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(null);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertTrue(result.hasError());
        assertEquals("Environment specific logging storage location is not supported for cloud platform: 'AWS'!", result.getErrors().get(0));
    }

    @Test
    public void validateTelemetryLoggingStorageLocationValidatorPasses() {
        when(environment.getTelemetry()).thenReturn(telemetry);
        when(telemetry.getLogging()).thenReturn(logging);
        when(logging.getStorageLocation()).thenReturn(REGION_1);
        when(loggingStorageLocationValidatorsByCloudPlatform.get(CloudPlatform.valueOf(CLOUD_PLATFORM))).thenReturn(validator);
        ValidationResult result = underTest.validateTelemetryLoggingStorageLocation(environment);
        assertFalse(result.hasError());
    }
}
