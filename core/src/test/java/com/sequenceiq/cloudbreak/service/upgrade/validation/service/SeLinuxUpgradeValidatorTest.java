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

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
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
        ServiceUpgradeValidationRequest request = ServiceUpgradeValidationRequest.builder()
                .withStack(stack)
                .withUpgradeImageInfo(UpgradeImageInfo.builder()
                        .withTargetStatedImage(mock())
                        .build())
                .build();
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);

        assertThrows(UpgradeValidationFailedException.class, () -> seLinuxUpgradeValidator.validate(request));

        verify(seLinuxValidationService, never()).validateSeLinuxSupportedOnTargetImage(any(StackDtoDelegate.class), any(Image.class));
    }

    @Test
    void testValidateWhenSeLinuxNotSupportedOnTargetImage() {
        StackDto stack = mock();
        Image targetImage = mock();
        ServiceUpgradeValidationRequest request = ServiceUpgradeValidationRequest.builder()
                .withStack(stack)
                .withUpgradeImageInfo(UpgradeImageInfo.builder()
                        .withTargetStatedImage(StatedImage.statedImage(targetImage, "not-relevant", "not-relevant"))
                        .build())
                .build();
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(stack, targetImage);

        assertThrows(UpgradeValidationFailedException.class, () -> seLinuxUpgradeValidator.validate(request));

        verify(seLinuxValidationService).validateSeLinuxEntitlementGranted(stack);
    }
}
