package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.ServiceUpgradeValidationRequestTestUtils;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;

@ExtendWith(MockitoExtension.class)
class SeLinuxUpgradeValidatorTest {

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    @InjectMocks
    private SeLinuxUpgradeValidator seLinuxUpgradeValidator;

    @Test
    void testValidateWhenEntitlementNotGranted() {
        StackDto stack = mock();
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ServiceUpgradeValidationRequest request = ServiceUpgradeValidationRequestTestUtils.of(stack, properties);
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);

        assertThrows(UpgradeValidationFailedException.class, () -> seLinuxUpgradeValidator.validate(request));

        verify(seLinuxValidationService, never()).validateSeLinuxSupportedOnTargetImage(any(StackDtoDelegate.class), any(ClusterUpgradeProperties.class));
    }

    @Test
    void testValidateWhenSeLinuxNotSupportedOnTargetImage() {
        StackDto stack = mock();
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withRuntimeVersion("7.2.18");
        ServiceUpgradeValidationRequest request = ServiceUpgradeValidationRequestTestUtils.of(stack, properties);
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, properties);

        assertThrows(UpgradeValidationFailedException.class, () -> seLinuxUpgradeValidator.validate(request));

        verify(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);
    }
}
