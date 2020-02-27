package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpgradeService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageService imageService;

    @Inject
    private StackUpgradeImageFilter stackUpgradeImageFilter;

    @Inject
    private UpgradeOptionsResponseFactory upgradeOptionsResponseFactory;

    @Inject
    private ImageProvider imageProvider;

    public UpgradeOptionsV4Response checkForUpgradesByName(Long workspaceId, String stackName) {
        UpgradeOptionsV4Response upgradeOptions = new UpgradeOptionsV4Response();
        try {
            Stack stack = stackService.getByNameInWorkspace(stackName, workspaceId);
            LOGGER.info(String.format("Retrieving images for upgrading stack %s", stack.getName()));
            com.sequenceiq.cloudbreak.cloud.model.Image currentImage = getImage(stack);
            CloudbreakImageCatalogV2 imageCatalog = getImagesFromCatalog(currentImage.getImageCatalogUrl());
            Image image = getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
            Images filteredImages = filterImages(imageCatalog, image, stack.cloudPlatform());
            LOGGER.info(String.format("%d possible image found for stack upgrade.", filteredImages.getCdhImages().size()));
            upgradeOptions = createResponse(image, filteredImages, stack.getCloudPlatform(), stack.getRegion(), currentImage.getImageCatalogName());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException | NotFoundException e) {
            LOGGER.error("Failed to get images", e);
            upgradeOptions.setReason(String.format("Failed to retrieve imaged due to %s", e.getMessage()));
        }
        return upgradeOptions;
    }

    private Image getCurrentImageFromCatalog(String currentImageId, CloudbreakImageCatalogV2 imageCatalog) throws CloudbreakImageNotFoundException {
        return imageProvider.getCurrentImageFromCatalog(currentImageId, imageCatalog);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getImage(Stack stack) throws CloudbreakImageNotFoundException {
        return imageService.getImage(stack.getId());
    }

    private CloudbreakImageCatalogV2 getImagesFromCatalog(String imageCatalogUrl) throws CloudbreakImageCatalogException {
        return imageCatalogProvider.getImageCatalogV2(imageCatalogUrl);
    }

    private Images filterImages(CloudbreakImageCatalogV2 imageCatalog, Image currentImage, String cloudPlatform) {
        return stackUpgradeImageFilter.filter(getCdhImages(imageCatalog), imageCatalog.getVersions(), currentImage, cloudPlatform);
    }

    private List<Image> getCdhImages(CloudbreakImageCatalogV2 imageCatalog) {
        return imageCatalog.getImages().getCdhImages();
    }

    private UpgradeOptionsV4Response createResponse(Image currentImage, Images filteredImages, String cloudPlatform, String region, String imageCatalogName) {
        return upgradeOptionsResponseFactory.createV4Response(currentImage, filteredImages, cloudPlatform, region, imageCatalogName);
    }
}
