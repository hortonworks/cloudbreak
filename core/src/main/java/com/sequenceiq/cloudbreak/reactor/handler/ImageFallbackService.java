package com.sequenceiq.cloudbreak.reactor.handler;

import static com.sequenceiq.common.model.OsType.RHEL8;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.ImageCatalogPlatform;

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
    private UserDataService userDataService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    public void fallbackToVhd(Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        StackView stackView = stackDtoService.getStackViewById(stackId);
        if (!CloudPlatform.AZURE.name().equals(stackView.getCloudPlatform())) {
            LOGGER.warn("Image fallback is only supported on the Azure cloud platform");
            return;
        }

        com.sequenceiq.cloudbreak.domain.stack.Component component = componentConfigProviderService.getImageComponent(stackId);
        Image currentImage = component.getAttributes().get(Image.class);

        if (RHEL8.getOs().equalsIgnoreCase(currentImage.getOsType()) && azureImageFormatValidator.isVhdImageFormat(currentImage)) {
            throw new CloudbreakServiceException("No valid fallback path from redhat8 VHD image.");
        }

        ImageCatalogPlatform platformString = platformStringTransformer.getPlatformStringForImageCatalog(
                stackView.getCloudPlatform(),
                stackView.getPlatformVariant());
        StatedImage image = imageCatalogService.getImage(stackView.getWorkspaceId(), currentImage.getImageCatalogUrl(),
                currentImage.getImageCatalogName(), currentImage.getImageId());
        String imageName = imageService.determineImageNameByRegion(stackView.getCloudPlatform(), platformString, stackView.getRegion(), image.getImage());
        Map<InstanceGroupType, String> userData = userDataService.getUserData(stackView.getId());

        component.setAttributes(new Json(new Image(imageName,
                new HashMap<>(),
                currentImage.getOs(),
                currentImage.getOsType(),
                currentImage.getImageCatalogUrl(),
                currentImage.getImageCatalogName(),
                currentImage.getImageId(),
                currentImage.getPackageVersions(),
                currentImage.getDate(),
                currentImage.getCreated())));

        userDataService.createOrUpdateUserData(stackId, userData);
        componentConfigProviderService.store(component);
    }
}
