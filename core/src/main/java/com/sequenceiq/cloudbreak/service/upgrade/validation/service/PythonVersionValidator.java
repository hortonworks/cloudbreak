package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;

@Component
public class PythonVersionValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonVersionValidator.class);

    @Inject
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        UpgradeImageInfo upgradeImageInfo = validationRequest.getUpgradeImageInfo();
        Image currentImage = upgradeImageInfo.getCurrentStatedImage().getImage();
        Image targetImage = upgradeImageInfo.getTargetStatedImage().getImage();
        if (isUpgradePermittedForRuntime(currentImage, targetImage)) {
            LOGGER.debug("Upgrade validation failed because the current image {} does not contains Python 3.8 and it's required for upgrade to the target"
                    + "image {}", currentImage.getUuid(), targetImage.getUuid());
            throw new UpgradeValidationFailedException(
                    "You are not eligible to upgrade to the selected runtime because an additional dependency must be present on your image. "
                            + "Please run an OS upgrade or upgrade to the latest service pack for your current runtime version first. "
                            + "This will automatically add the required dependency. "
                            + "After this you will be able to launch another upgrade to the more recent runtime.");
        }
    }

    private boolean isUpgradePermittedForRuntime(Image currentImage, Image targetImage) {
        return !pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(currentImage, targetImage);
    }
}
