package com.sequenceiq.externalizedcompute.service.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeEntitlementValidatorTest {

    private static final String ACCOUNT = "test-account";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ExternalizedComputeEntitlementValidator underTest;

    @Test
    void testValidateWhenEntitlementEnabled() {
        when(entitlementService.isComputeClusterEnabled(ACCOUNT)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateComputeClusterEntitlement(ACCOUNT));
    }

    @Test
    void testValidateWhenEntitlementDisabled() {
        when(entitlementService.isComputeClusterEnabled(ACCOUNT)).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> underTest.validateComputeClusterEntitlement(ACCOUNT));
        assertEquals("You are not entitled to use externalized compute cluster. "
                + "Please contact Cloudera to enable ENABLE_COMPUTE_CLUSTER for your account.", ex.getMessage());
    }
}
