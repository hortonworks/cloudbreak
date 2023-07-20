package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;

@Component
public class PythonVersionValidator implements ServiceUpgradeValidator {

    public static final String ERROR_MESSAGE =
            "You are not eligible to upgrade to the selected runtime because an additional dependency must be present on your image. "
                    + "Please run an OS upgrade or upgrade to the latest service pack for your current runtime version first. "
                    + "This will automatically add the required dependency. "
                    + "After this you will be able to launch another upgrade to the more recent runtime.";

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonVersionValidator.class);

    @Inject
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        UpgradeImageInfo upgradeImageInfo = validationRequest.getUpgradeImageInfo();
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = upgradeImageInfo.currentImage();
        Image targetImage = upgradeImageInfo.targetStatedImage().getImage();
        if (isUpgradeDeniedForRuntime(validationRequest.getStack(), upgradeImageInfo.currentImage().getImageCatalogName(), currentImage, targetImage)) {
            LOGGER.debug("Upgrade validation failed because the current image {} does not contains Python 3.8 and it's required for upgrade to the target"
                    + "image {}", currentImage.getImageId(), targetImage.getUuid());
            throw new UpgradeValidationFailedException(ERROR_MESSAGE);
        }
    }

    private boolean isUpgradeDeniedForRuntime(StackDto stack, String imageCatalogName, com.sequenceiq.cloudbreak.cloud.model.Image currentImage,
            Image targetImage) {
        List<Image> cdhImagesFromCatalog = getAllCdhImagesFromCatalog(stack, imageCatalogName);
        return !pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, cdhImagesFromCatalog, currentImage, targetImage);
    }

    private List<Image> getAllCdhImagesFromCatalog(StackDto stack, String imageCatalogName) {
        try {
            return imageCatalogService.getAllCdhImages(stack.getWorkspaceId(), imageCatalogName,
                    platformStringTransformer.getPlatformStringForImageCatalogSet(stack.getCloudPlatform(), stack.getPlatformVariant()));
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Failed to retrieve images from catalog {}", imageCatalogName, e);
            throw new CloudbreakServiceException(e);
        }
    }
}
