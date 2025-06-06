package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Collections;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    @Inject
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    @Inject
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Inject
    private ImageCatalogService imageCatalogService;

    public ImageFilterResult getAvailableImagesForUpgrade(Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        if (isBaseImage(imageFilterParams.getCurrentImage())) {
            LOGGER.info(String.format("Base image found, upgrade not supported: %s", imageFilterParams.getCurrentImage()));
            return createEmptyResult("Cannot upgrade a base image cluster.");
        } else {
            BlueprintValidationResult blueprintValidationResult = isValidBlueprint(imageFilterParams);
            return blueprintValidationResult.isValid() ? getImageFilterResult(workspaceId, imageCatalogName, imageFilterParams)
                    : createEmptyResult(blueprintValidationResult.getReason());
        }
    }

    private boolean isBaseImage(Image image) {
        String cdhBuildNumber = image.getPackageVersion(ImagePackageVersion.CDH_BUILD_NUMBER);
        String cmVersion = image.getPackageVersion(ImagePackageVersion.CM);
        return StringUtils.isEmpty(cdhBuildNumber) || StringUtils.isEmpty(cmVersion);
    }

    private ImageFilterResult getImageFilterResult(Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        try {
            ImageFilterResult availableImages = imageCatalogService.getImageFilterResult(workspaceId, imageCatalogName,
                    imageFilterParams.getImageCatalogPlatform(), imageFilterParams.isGetAllImages(), imageFilterParams.getCurrentImage().getImageId());
            return availableImages.getImages().isEmpty() ? availableImages : filterImages(availableImages, imageFilterParams);
        } catch (Exception ex) {
            LOGGER.error("Error during image filtering.", ex);
            return createEmptyResult("Failed to retrieve eligible images due to an internal error.");
        }
    }

    private ImageFilterResult filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        LOGGER.debug("Starting to filter {} upgrade candidate image.", imageFilterResult.getImages().size());
        return imageFilterUpgradeService.filterImages(imageFilterResult, imageFilterParams);
    }

    private BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams) {
        return blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams);
    }

    private ImageFilterResult createEmptyResult(String reason) {
        return new ImageFilterResult(Collections.emptyList(), reason);
    }
}
