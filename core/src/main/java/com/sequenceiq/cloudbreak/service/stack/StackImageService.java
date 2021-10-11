package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
public class StackImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageService.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public void storeNewImageComponent(Stack stack, StatedImage targetImage) {
        try {
            replaceStackImageComponent(stack, targetImage);
        } catch (IllegalArgumentException e) {
            LOGGER.info("Failed to create json", e);
            throw new CloudbreakServiceException("Failed to create json", e);
        }
    }

    public Image getImageModelFromStatedImage(Stack stack, Image currentImage, StatedImage targetImage) {
        try {
            String platformString = platform(stack.cloudPlatform()).value().toLowerCase();
            String newImageName = imageService.determineImageName(platformString, stack.getRegion(), targetImage.getImage());
            return new Image(newImageName, currentImage.getUserdata(), targetImage.getImage().getOs(), targetImage.getImage().getOsType(),
                    targetImage.getImageCatalogUrl(), targetImage.getImageCatalogName(), targetImage.getImage().getUuid(),
                    targetImage.getImage().getPackageVersions());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Could not find image", e);
            throw new CloudbreakServiceException("Could not find image", e);
        }
    }

    public void removeImageByComponentName(Long stackId, String imageComponentName) {
        Optional<Component> imageComponent = findImageComponentByName(stackId, imageComponentName);
        if (imageComponent.isPresent()) {
            LOGGER.debug("Deleting image component {} from stack {}", imageComponentName, stackId);
            componentConfigProviderService.deleteComponents(Collections.singleton(imageComponent.get()));
        } else {
            LOGGER.warn("There is no image component found for stack {} with name {}", stackId, imageComponentName);
        }
    }

    public Image getCurrentImage(Stack stack) throws CloudbreakImageNotFoundException {
            return componentConfigProviderService.getImage(stack.getId());
    }

    public void changeImageCatalog(Stack stack, String imageCatalog) {
        try {
            Image currentImage = componentConfigProviderService.getImage(stack.getId());
            ImageCatalog targetImageCatalog = imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), imageCatalog);
            StatedImage targetImage = imageCatalogService.getImage(
                    targetImageCatalog.getImageCatalogUrl(),
                    targetImageCatalog.getName(),
                    currentImage.getImageId()
            );
            replaceStackImageComponent(stack, targetImage);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to change the image catalog.", e);
            throw new CloudbreakServiceException("Failed to change the image catalog.");
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new NotFoundException(e.getMessage());
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Failed to replace stack image component", e);
            throw new CloudbreakServiceException(e.getMessage());
        }
    }

    private void replaceStackImageComponent(Stack stack, StatedImage targetImage) {
        try {
            Image newImage = getImageModelFromStatedImage(stack, componentConfigProviderService.getImage(stack.getId()), targetImage);
            Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(newImage), stack);
            componentConfigProviderService.replaceImageComponentWithNew(imageComponent);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Could not find image", e);
            throw new CloudbreakServiceException("Could not find image", e);
        }
    }

    public Optional<Component> findImageComponentByName(Long stackId, String componentName) {
        Component targetImageComponent = componentConfigProviderService.getComponent(stackId, ComponentType.IMAGE, componentName);
        LOGGER.debug("The following target image found for stack {}, {}", stackId, targetImageComponent);
        return Optional.ofNullable(targetImageComponent);
    }
}
