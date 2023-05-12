package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.CurrentImagePackageProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentService;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class PythonVersionBasedRuntimeVersionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonVersionBasedRuntimeVersionValidator.class);

    private static final String MINIMUM_RUNTIME_VERSION_FOR_DATA_HUB = "7.2.16";

    private static final String MINIMUM_RUNTIME_VERSION_FOR_DATA_LAKE = "7.2.17";

    @Inject
    private LockedComponentService lockedComponentService;

    @Inject
    private CurrentImagePackageProvider currentImagePackageProvider;

    public boolean isUpgradePermittedForRuntime(StackDto stack, List<Image> cdhImagesFromCatalog, Image currentImage, Image targetImage) {
        String targetImageId = targetImage.getUuid();
        if (isTargetRuntimeRequiresPython38(targetImage, stack)) {
            if (isCurrentImageContainsPython38(stack, cdhImagesFromCatalog, currentImage) || isOsUpgrade(stack, currentImage, targetImage)) {
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

    private boolean isOsUpgrade(StackDto stack, Image currentImage, Image targetImage) {
        return currentImage.getStackDetails().getVersion().equals(targetImage.getStackDetails().getVersion()) &&
                lockedComponentService.isComponentsLocked(stack, currentImage, targetImage);
    }

    private boolean isTargetRuntimeRequiresPython38(Image targetImage, StackDto stack) {
        String targetRuntimeVersion = targetImage.getStackDetails().getVersion();
        return new VersionComparator().compare(() -> targetRuntimeVersion, () -> getMinimumRuntimeVersion(stack)) >= 0;
    }

    private String getMinimumRuntimeVersion(StackDto stack) {
        return isDataHubCluster(stack) ? MINIMUM_RUNTIME_VERSION_FOR_DATA_HUB : MINIMUM_RUNTIME_VERSION_FOR_DATA_LAKE;
    }

    private boolean isDataHubCluster(StackDto stack) {
        return stack.getType().equals(StackType.WORKLOAD);
    }

    private boolean isCurrentImageContainsPython38(StackDto stack, List<Image> cdhImagesFromCatalog, Image currentImage) {
        return currentImage.getPackageVersions().containsKey(ImagePackageVersion.PYTHON38.getKey())
                && currentImagePackageProvider.currentInstancesContainsPackage(stack.getId(), cdhImagesFromCatalog, ImagePackageVersion.PYTHON38);
    }
}
