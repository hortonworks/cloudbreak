package com.sequenceiq.cloudbreak.service.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class DatabaseProvisioningValidatorTest {

    @InjectMocks
    private DatabaseProvisioningValidator underTest;

    @Mock
    private DbOverrideConfig dbOverrideConfig;

    @Test
    void testValidateThrowsWhenDeprecatedVersionWithSuggestion() {
        when(dbOverrideConfig.isVersionSupportedForRuntime("11", "7.3.2")).thenReturn(false);
        when(dbOverrideConfig.findEngineVersionForRuntime("7.3.2")).thenReturn("14");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateForProvisioning("11", "7.3.2"));
        assertTrue(exception.getMessage().contains("not supported for runtime 7.3.2"));
        assertTrue(exception.getMessage().contains("Please use PostgreSQL 14 or later."));
    }

    @Test
    void testValidateThrowsWhenDeprecatedVersionWithoutSuggestion() {
        when(dbOverrideConfig.isVersionSupportedForRuntime("11", "7.3.2")).thenReturn(false);
        when(dbOverrideConfig.findEngineVersionForRuntime("7.3.2")).thenReturn(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateForProvisioning("11", "7.3.2"));
        assertTrue(exception.getMessage().contains("not supported for runtime 7.3.2"));
        assertTrue(exception.getMessage().contains("Please use a supported version."));
    }

    @Test
    void testValidatePassesWhenSupported() {
        when(dbOverrideConfig.isVersionSupportedForRuntime("14", "7.2.7")).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateForProvisioning("14", "7.2.7"));
    }
}
