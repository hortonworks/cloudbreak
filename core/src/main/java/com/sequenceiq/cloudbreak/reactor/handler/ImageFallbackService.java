package com.sequenceiq.cloudbreak.reactor.handler;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogPlatform;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ImageFallbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFallbackService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ImageService imageService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    public void fallbackToVhd(Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        StackView stackView = stackDtoService.getStackViewById(stackId);
        if (!CloudPlatform.AZURE.name().equals(stackView.getCloudPlatform())) {
            LOGGER.warn("Image fallback is only supported on the Azure cloud platform");
            return;
        }

        com.sequenceiq.cloudbreak.domain.stack.Component component = componentConfigProviderService.getImageComponent(stackId);
        Image currentImage = component.getAttributes().get(Image.class);

        ImageCatalogPlatform platformString = platformStringTransformer.getPlatformStringForImageCatalog(
                stackView.getCloudPlatform(),
                stackView.getPlatformVariant());
        StatedImage image = imageCatalogService.getImage(stackView.getWorkspaceId(), currentImage.getImageCatalogUrl(),
                currentImage.getImageCatalogName(), currentImage.getImageId());
        String imageName = imageService.determineImageNameByRegion(stackView.getCloudPlatform(), platformString, stackView.getRegion(), image.getImage());

        component.setAttributes(new Json(new Image(imageName,
                currentImage.getUserdata(),
                currentImage.getOs(),
                currentImage.getOsType(),
                currentImage.getImageCatalogUrl(),
                currentImage.getImageCatalogName(),
                currentImage.getImageId(),
                currentImage.getPackageVersions())));
        componentConfigProviderService.store(component);
    }
}
