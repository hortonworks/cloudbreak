package com.sequenceiq.cloudbreak.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.common.model.SeLinux;

@ExtendWith(MockitoExtension.class)
class SeLinuxValidationServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SeLinuxValidationService underTest;

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenSecurityConfigNull() {
        StackDto stack = mock();

        assertDoesNotThrow(() -> underTest.validateSeLinuxEntitlementGranted(stack));

        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenSeLinuxNull() {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);

        assertDoesNotThrow(() -> underTest.validateSeLinuxEntitlementGranted(stack));

        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedWhenPermissive() {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.PERMISSIVE);

        assertDoesNotThrow(() -> underTest.validateSeLinuxEntitlementGranted(stack));

        verifyNoInteractions(entitlementService);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testValidateSeLinuxEntitlementGrantedWhenEnforcing(boolean entitled) {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        when(entitlementService.isCdpSecurityEnforcingSELinux("accid")).thenReturn(entitled);

        if (entitled) {
            assertDoesNotThrow(() ->
                    ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxEntitlementGranted(stack)));
        } else {
            assertThrows(CloudbreakServiceException.class, () ->
                    ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateSeLinuxEntitlementGranted(stack)));
        }
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenPermissive() {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.PERMISSIVE);
        Image image = mock();
        ImageStackDetails imageStackDetails = mock();
        when(image.getStackDetails()).thenReturn(imageStackDetails);

        assertDoesNotThrow(() -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image));
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
                // imageSupportsSeLinux, stackVersion, shouldPass
                Arguments.of(true, "7.2.17", false),
                Arguments.of(true, "7.2.18", true),
                Arguments.of(true, "7.3.0", true),
                Arguments.of(false, "7.2.17", false),
                Arguments.of(false, "7.2.18", false),
                Arguments.of(false, "7.3.0", false)
        );
    }

    @MethodSource("arguments")
    @ParameterizedTest
    void testValidateSeLinuxSupportedOnTargetImageWhenEnforcing(boolean imageSupportsSeLinux, String stackVersion, boolean shouldPass) {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        Image image = mock();
        ImageStackDetails imageStackDetails = mock();
        when(imageStackDetails.getVersion()).thenReturn(stackVersion);
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        if (imageSupportsSeLinux) {
            when(image.getTags()).thenReturn(Map.of());
        } else {
            when(image.getTags()).thenReturn(Map.of("selinux-supported", "false"));
        }

        if (shouldPass) {
            assertDoesNotThrow(() -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image));
        } else {
            assertThrows(CloudbreakServiceException.class, () -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image));
        }
    }

    @MethodSource("arguments")
    @ParameterizedTest
    void testValidateSeLinuxSupportedOnTargetImageWhenEnforcingOtherImageType(boolean imageSupportsSeLinux, String stackVersion, boolean shouldPass) {
        com.sequenceiq.cloudbreak.cloud.model.Image image = mock();
        when(image.getPackageVersions()).thenReturn(Map.of(ImagePackageVersion.STACK.getKey(), stackVersion));
        if (imageSupportsSeLinux) {
            when(image.getTags()).thenReturn(Map.of());
        } else {
            when(image.getTags()).thenReturn(Map.of("selinux-supported", "false"));
        }

        if (shouldPass) {
            assertDoesNotThrow(() -> underTest.validateSeLinuxSupportedOnTargetImage(SeLinux.ENFORCING, image));
        } else {
            assertThrows(CloudbreakServiceException.class, () -> underTest.validateSeLinuxSupportedOnTargetImage(SeLinux.ENFORCING, image));
        }
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenCantDetermineStackVersion() {
        com.sequenceiq.cloudbreak.cloud.model.Image image = mock();
        when(image.getPackageVersions()).thenReturn(Map.of());

        assertDoesNotThrow(() -> underTest.validateSeLinuxSupportedOnTargetImage(SeLinux.ENFORCING, image));
    }

    @Test
    void testValidateSeLinuxSupportedOnTargetImageWhenImageStackDetailsIsNull() {
        StackDto stack = mock();
        SecurityConfig securityConfig = mock();
        when(stack.getSecurityConfig()).thenReturn(securityConfig);
        when(securityConfig.getSeLinux()).thenReturn(SeLinux.ENFORCING);
        Image image = mock();
        when(image.getStackDetails()).thenReturn(null);

        assertDoesNotThrow(() -> underTest.validateSeLinuxSupportedOnTargetImage(stack, image));
    }
}
