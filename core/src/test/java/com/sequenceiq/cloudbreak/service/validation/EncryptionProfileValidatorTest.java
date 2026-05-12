package com.sequenceiq.cloudbreak.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EncryptionProfileValidator underTest;

    @Test
    void testEntitlement() {
        StackDto stack = new StackDto();
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stack)));

        assertEquals("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.", ex.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenRuntimeIsBellow732() {
        StackDto stack = mock(StackDto.class);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stack.getStackVersion()).thenReturn("7.3.1");

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stack)));

        assertEquals("Encryption profile feature requires runtime 7.3.2 or above", ex.getMessage());
    }

    @Test
    void testValidateShouldWorkFor732AndClusterAvailable() {
        StackDto stack = mock(StackDto.class);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);

        assertDoesNotThrow(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stack)));
    }

    @Test
    void testValidateShouldThrowExceptionWhenStatusIsNotAvailable() {
        StackDto stack = mock(StackDto.class);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getStatus()).thenReturn(Status.STOP_IN_PROGRESS);

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stack)));

        assertEquals("Cluster need to be in AVAILABLE state to enable encryption profile. Status: STOP_IN_PROGRESS", ex.getMessage());
    }
}