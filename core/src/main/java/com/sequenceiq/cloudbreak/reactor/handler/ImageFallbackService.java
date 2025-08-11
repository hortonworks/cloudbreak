package com.sequenceiq.cloudbreak.reactor.handler;

import static com.sequenceiq.common.model.OsType.RHEL8;

import java.io.IOException;
import java.util.HashMap;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
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
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class ImageFallbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFallbackService.class);

    @Inject
    private StackDtoService stackService;

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

    @Inject
    private EntitlementService entitlementService;

    public void fallbackToVhd(Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        StackView stackView = stackService.getStackViewById(stackId);
        com.sequenceiq.cloudbreak.domain.stack.Component component = componentConfigProviderService.getImageComponent(stackId);
        Image currentImage = component.getAttributes().get(Image.class);
        if (imageFallbackPermitted(currentImage, stackView)) {
            if (RHEL8.getOs().equalsIgnoreCase(currentImage.getOsType()) && azureImageFormatValidator.isVhdImageFormat(currentImage.getImageName())) {
                String message = String.format("Failed to start instances with image: %s. The current image is a Redhat 8 VHD image, " +
                                "please check if the source image is signed.",
                        currentImage.getImageName());
                throw new CloudbreakServiceException(message);
            }
            String fallbackImageName = null;
            try {
                fallbackImageName = getFallbackImageName(stackView, currentImage);
            } catch (CloudbreakImageNotFoundException e) {
                String errorMessage = String.format("Your image %s seems to be an Azure Marketplace image, "
                        + "however its Terms and Conditions are not accepted! "
                        + "Please either enable automatic consent or accept the terms manually and initiate the provisioning or upgrade again. " +
                        "On how to accept the Terms and Conditions of the image please refer to azure documentation " +
                        "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.", currentImage.getImageName());
                throw new CloudbreakServiceException(errorMessage);
            }
            LOGGER.debug("Replacing current image {} with fallback image {}", currentImage.getImageName(), fallbackImageName);
            userDataService.makeSureUserDataIsMigrated(stackView.getId());
            component.setAttributes(new Json(new Image(fallbackImageName,
                    new HashMap<>(),
                    currentImage.getOs(),
                    currentImage.getOsType(),
                    currentImage.getArchitecture(),
                    currentImage.getImageCatalogUrl(),
                    currentImage.getImageCatalogName(),
                    currentImage.getImageId(),
                    currentImage.getPackageVersions(),
                    currentImage.getDate(),
                    currentImage.getCreated(),
                    currentImage.getTags())));
            componentConfigProviderService.store(component);
        }
    }

    public String getFallbackImageName(StackView stack, Image currentImage) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        if (!imageFallbackPermitted(currentImage, stack)) {
            return null;
        } else {
            ImageCatalogPlatform platformString = platformStringTransformer.getPlatformStringForImageCatalog(stack.getCloudPlatform(),
                    stack.getPlatformVariant());
            StatedImage image = imageCatalogService.getImage(stack.getWorkspaceId(), currentImage.getImageCatalogUrl(),
                    currentImage.getImageCatalogName(), currentImage.getImageId());
            String fallbackImageName = imageService.determineImageNameByRegion(stack.getCloudPlatform(), platformString, stack.getRegion(), image.getImage());
            LOGGER.debug("Determined fallback image name is {}", fallbackImageName);
            return fallbackImageName;
        }
    }

    private boolean imageFallbackPermitted(Image currentImage, StackView stack) {
        String accountId = Crn.fromString(stack.getResourceCrn()).getAccountId();
        boolean hasAzurePlatform = CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform());
        boolean hasMarketplaceImageFormat = azureImageFormatValidator.isMarketplaceImageFormat(currentImage.getImageName());
        boolean onlyMarketplaceImagesEnabled = entitlementService.azureOnlyMarketplaceImagesEnabled(accountId);

        if (hasAzurePlatform && hasMarketplaceImageFormat && !onlyMarketplaceImagesEnabled) {
            return true;
        } else {
            String msg = String.format("Image fallback for image %s is not permitted! Cloud platform: %s, Marketplace image: %s,  MP only entitlement: %b",
                    currentImage.getImageName(),
                    stack.getCloudPlatform(),
                    hasMarketplaceImageFormat,
                    onlyMarketplaceImagesEnabled);
            LOGGER.warn(msg);
            return false;
        }
    }
}