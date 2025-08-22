package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;

@Component
public class SeLinuxUpgradeValidator implements ServiceUpgradeValidator {

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        try {
            StackDto stack = validationRequest.stack();
            Image targetImage = validationRequest.upgradeImageInfo().targetStatedImage().getImage();

            seLinuxValidationService.validateSeLinuxEntitlementGranted(stack);
            seLinuxValidationService.validateSeLinuxSupportedOnTargetImage(stack, targetImage);
        } catch (CloudbreakServiceException e) {
            throw new UpgradeValidationFailedException(e);
        }
    }
}
