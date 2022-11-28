package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@Component
public class ClusterUpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeImageFilter.class);

    @Inject
    private BlueprintBasedUpgradeValidator blueprintBasedUpgradeValidator;

    @Inject
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Inject
    private ImageCatalogService imageCatalogService;

    public ImageFilterResult filter(String accountId, Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        BlueprintValidationResult blueprintValidationResult = isValidBlueprint(imageFilterParams, accountId);
        return blueprintValidationResult.isValid() ? getImageFilterResult(workspaceId, imageCatalogName, imageFilterParams)
                : createEmptyResult(blueprintValidationResult.getReason());
    }

    private ImageFilterResult getImageFilterResult(Long workspaceId, String imageCatalogName, ImageFilterParams imageFilterParams) {
        try {
            ImageFilterResult imageFilterResult = imageCatalogService
                    .getImageFilterResult(workspaceId, imageCatalogName, imageFilterParams.getImageCatalogPlatform(), imageFilterParams.isGetAllImages());
            return imageFilterResult.getImages().isEmpty() ? imageFilterResult : filterImages(imageFilterResult, imageFilterParams);
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
