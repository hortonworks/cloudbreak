package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EncryptionProfileValidator underTest;

    @Test
    void testEntitlement() {
        List<StackViewV4Response> stacks = List.of();
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stacks)));

        assertEquals("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.", ex.getMessage());
    }

    @Test
    void testRuntimeVersion() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.1");
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.2");
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        stack3.setStackVersion("7.2.18");
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        stack4.setStackVersion("7.3.3");
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stacks)));

        assertEquals("All clusters runtime need to be 7.3.2 or above to enable encryption profile. Upgrade cluster(s): stack1,stack3", ex.getMessage());
    }

    @Test
    void testStackStatus() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.3");
        stack1.setStatus(Status.AVAILABLE);
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.4");
        stack2.setStatus(Status.STOPPED);
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        stack3.setStackVersion("7.3.2");
        stack3.setStatus(Status.DELETE_FAILED);
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        stack4.setStackVersion("7.4.0");
        stack4.setStatus(Status.AVAILABLE);
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(stacks)));

        assertEquals("All clusters need to be available to enable encryption profile. Cluster(s) not available: stack2,stack3", ex.getMessage());
    }
}