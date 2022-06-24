package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    @Inject
    private BlueprintBasedUpgradeValidator blueprintBasedUpgradeValidator;

    @Inject
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Inject
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Inject
    private ImageCatalogService imageCatalogService;

    public ImageFilterResult filter(String accountId, CloudbreakImageCatalogV3 imageCatalogV3, ImageFilterParams imageFilterParams) {
        BlueprintValidationResult blueprintValidationResult = isValidBlueprint(imageFilterParams, accountId);
        return blueprintValidationResult.isValid() ? getImageFilterResult(imageCatalogV3, imageFilterParams)
                : createEmptyResult(blueprintValidationResult.getReason());
    }

    public ImageFilterResult filter(String accountId, Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        BlueprintValidationResult blueprintValidationResult = isValidBlueprint(imageFilterParams, accountId);
        return blueprintValidationResult.isValid() ? getImageFilterResult(workspaceId, imageCatalogName, imageFilterParams)
                : createEmptyResult(blueprintValidationResult.getReason());
    }

    private ImageFilterResult getImageFilterResult(CloudbreakImageCatalogV3 imageCatalogV3, ImageFilterParams imageFilterParams) {
        ImageFilterResult imagesForCbVersion = imageCatalogServiceProxy.getImageFilterResult(imageCatalogV3);
        return imagesForCbVersion.getImages().isEmpty() ? imagesForCbVersion : filterImages(imagesForCbVersion, imageFilterParams);
    }

    private ImageFilterResult getImageFilterResult(Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        try {
            StatedImages statedImages = imageCatalogService.getImages(workspaceId, imageCatalogName, imageFilterParams.getCloudPlatform());
            ImageFilterResult imageFilterResult = new ImageFilterResult(statedImages.getImages().getCdhImages());
            return filterImages(imageFilterResult, imageFilterParams);
        } catch (Exception ex) {
            LOGGER.error("Error during image filtering.", ex);
            return createEmptyResult("Failed to retrieve eligible images due tue an internal error.");
        }
    }

    private ImageFilterResult filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        LOGGER.debug("Starting to filter {} upgrade candidate image.", imageFilterResult.getImages().size());
        return imageFilterUpgradeService.filterImages(imageFilterResult, imageFilterParams);
    }

    private BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams, String accountId) {
        return blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, accountId);
    }

    private ImageFilterResult createEmptyResult(String reason) {
        return new ImageFilterResult(Collections.emptyList(), reason);
    }
}
