package com.sequenceiq.cloudbreak.service.upgrade.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class PythonVersionBasedRuntimeVersionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonVersionBasedRuntimeVersionValidator.class);

    private static final String MINIMUM_RUNTIME_VERSION = "7.2.16";

    public boolean isUpgradePermittedForRuntime(Image currentImage, Image targetImage) {
        String targetImageId = targetImage.getUuid();
        if (isTargetImageRequiresPython38(targetImage)) {
            if (isCurrentImageContainsPython38(currentImage)) {
                LOGGER.debug("Permitting upgrade for image {} because the required Python version is present on the current image {}", targetImageId,
                        currentImage.getUuid());
                return true;
            } else {
                LOGGER.debug("The upgrade is not possible for image {} because the target runtime requires Python 3.8 dependency", targetImageId);
                return false;
            }
        }
        LOGGER.debug("Permitting upgrade because the target image {} does not require Python 3.8 dependency", targetImageId);
        return true;
    }

    private boolean isTargetImageRequiresPython38(Image targetImage) {
        return isTargetImageContainsPython38(targetImage) && isTargetVersionRequiresPython38(targetImage.getStackDetails().getVersion());
    }

    private boolean isTargetVersionRequiresPython38(String targetRuntimeVersion) {
        return new VersionComparator().compare(() -> targetRuntimeVersion, () -> MINIMUM_RUNTIME_VERSION) >= 0;
    }

    private boolean isTargetImageContainsPython38(Image targetImage) {
        return targetImage.getPackageVersions().containsKey(ImagePackageVersion.PYTHON38.getKey());
    }

    private boolean isCurrentImageContainsPython38(Image currentImage) {
        return currentImage.getPackageVersions().containsKey(ImagePackageVersion.PYTHON38.getKey());
    }
}
