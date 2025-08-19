package com.sequenceiq.freeipa.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class SeLinuxValidationServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SeLinuxValidationService underTest;

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenEnforcingAndEntitled() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(entitlementService.isCdpSecurityEnforcingSELinux("accid")).thenReturn(true);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxEntitlementGranted(stack)));
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenEnforcingAndNotEntitled() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(entitlementService.isCdpSecurityEnforcingSELinux("accid")).thenReturn(false);

        assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxEntitlementGranted(stack)));
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenPermissive() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxEntitlementGranted(stack)));

        verifyNoInteractions(entitlementService);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testValidateSeLinuxSupportedOnTargetImageWhenEnforcingAndSupported(boolean selinuxSupportedTagPresent) {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        Map<String, String> imageTags = new HashMap<>();
        if (selinuxSupportedTagPresent) {
            imageTags.put("selinux-supported", Boolean.TRUE.toString());
        }
        Image image = mock(Image.class);
        when(image.getTags()).thenReturn(imageTags);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image)));
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenEnforcingButNotSupported() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        Image image = mock(Image.class);
        when(image.getTags()).thenReturn(Map.of("selinux-supported", Boolean.FALSE.toString()));

        assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image)));
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenEnforcingButNotSupportedWithOtherImageType() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image image = mock(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image.class);
        when(image.getTags()).thenReturn(Map.of("selinux-supported", Boolean.FALSE.toString()));

        assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image)));
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenPermissive() {
        Stack stack = mock(Stack.class);
        SecurityConfig securityConfig = mock(SecurityConfig.class);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.PERMISSIVE);
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        Image image = mock(Image.class);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image)));
    }
}
