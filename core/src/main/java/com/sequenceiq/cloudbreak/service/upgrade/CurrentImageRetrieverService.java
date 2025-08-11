package com.sequenceiq.cloudbreak.service.upgrade;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;

@Component
public class CurrentImageRetrieverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImageRetrieverService.class);

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageService imageService;

    public Image retrieveCurrentModelImage(Stack stack) throws CloudbreakImageNotFoundException {
        Image currentImage = getImage(stack);
        String imageId = currentImage.getImageId();
        String imageCatalogUrl = currentImage.getImageCatalogUrl();
        try {
            String imageCatalogName = currentImage.getImageCatalogName();
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogImage = imageCatalogService.getImage(stack.getWorkspace().getId(), imageCatalogUrl,
                    imageCatalogName, imageId).getImage();
            LOGGER.debug("Using the catalog image to create the model of the current image.");
            return new Image(currentImage.getImageName(), currentImage.getUserdata(), catalogImage.getOs(), catalogImage.getOsType(),
                    catalogImage.getArchitecture(), imageCatalogUrl, imageCatalogName, imageId, catalogImage.getPackageVersions(), catalogImage.getDate(),
                    catalogImage.getCreated(), catalogImage.getTags());
        } catch (CloudbreakImageCatalogException | CloudbreakImageNotFoundException e) {
            LOGGER.warn("Failed to retrieve the current image {} from image catalog {}. Falling back to the image from the database.",
                    imageId, imageCatalogUrl);
            return currentImage;
        }
    }

    private Image getImage(Stack stack) throws CloudbreakImageNotFoundException {
        return imageService.getImage(stack.getId());
    }
}
