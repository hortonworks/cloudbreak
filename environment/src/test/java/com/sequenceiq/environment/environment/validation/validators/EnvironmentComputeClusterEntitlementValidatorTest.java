package com.sequenceiq.environment.environment.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class EnvironmentComputeClusterEntitlementValidatorTest {

    private static final String ACCOUNT = "test-account";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EnvironmentComputeClusterEntitlementValidator underTest;

    @Test
    void testValidateWhenEntitlementEnabled() {
        when(entitlementService.isComputeClusterEnabled(ACCOUNT)).thenReturn(true);

        ValidationResult result = underTest.validate(ACCOUNT);

        assertFalse(result.hasError());
    }

    @Test
    void testValidateWhenEntitlementDisabled() {
        when(entitlementService.isComputeClusterEnabled(ACCOUNT)).thenReturn(false);

        ValidationResult result = underTest.validate(ACCOUNT);

        assertTrue(result.hasError());
        assertThat(result.getErrors()).containsOnly(
                "You are not entitled to use externalized compute cluster. "
                        + "Please contact Cloudera to enable ENABLE_COMPUTE_CLUSTER for your account.");
    }
}
