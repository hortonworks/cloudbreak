package com.sequenceiq.environment.environment.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@ExtendWith(MockitoExtension.class)
class SeLinuxValidationServiceTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SeLinuxValidationService underTest;

    @Test
    void testValidateSeLinuxEntitlementGrantedForFreeipaCreationWhenEnforcingAndEntitled() {
        when(entitlementService.isCdpSecurityEnforcingSELinux("accid")).thenReturn(true);
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(SeLinux.ENFORCING)
                .build();

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.validateSeLinuxEntitlementGrantedForFreeipaCreation(freeIpaCreationDto)));
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedForFreeipaCreationWhenEnforcingAndNotEntitled() {
        when(entitlementService.isCdpSecurityEnforcingSELinux("accid")).thenReturn(false);
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(SeLinux.ENFORCING)
                .build();

        assertThrows(CloudbreakServiceException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                        underTest.validateSeLinuxEntitlementGrantedForFreeipaCreation(freeIpaCreationDto)));
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedForFreeipaCreationWhenPermissive() {
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(SeLinux.PERMISSIVE)
                .build();

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.validateSeLinuxEntitlementGrantedForFreeipaCreation(freeIpaCreationDto)));
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateSeLinuxEntitlementGrantedForFreeipaCreationWhenNull() {
        FreeIpaCreationDto freeIpaCreationDto = FreeIpaCreationDto.builder(1)
                .withSeLinux(null)
                .build();

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.validateSeLinuxEntitlementGrantedForFreeipaCreation(freeIpaCreationDto)));
        verifyNoInteractions(entitlementService);
    }
}
