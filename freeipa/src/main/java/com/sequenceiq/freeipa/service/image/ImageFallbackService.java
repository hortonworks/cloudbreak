package com.sequenceiq.freeipa.service.image;

import java.util.Locale;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Service
public class ImageFallbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFallbackService.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ImageProviderFactory imageProviderFactory;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    public boolean imageFallbackPermitted(ImageEntity currentImage, Stack stack) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform()) &&
                azureImageFormatValidator.isMarketplaceImageFormat(currentImage.getImageName())) {
            return true;
        } else {
            String msg = String.format("Image fallback is only supported on the Azure cloud platform and Marketplace image %s", currentImage.getImageName());
            LOGGER.info(msg);
            return false;
        }
    }

    public Optional<String> determineFallbackImageIfPermitted(StackContext context) {
        Stack stack = context.getStack();
        CloudContext cloudContext = context.getCloudContext();
        ImageEntity imageEntity = imageService.getByStack(stack);
        String regionName = cloudContext.getLocation().getRegion().value();
        String platform = cloudContext.getPlatform().getValue();
        if (imageFallbackPermitted(imageEntity, stack)) {
            try {
                com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image imageForStack = imageService.getImageForStack(stack);
                String fallbackImageName = imageService.determineImageNameByRegion(platform, regionName, imageForStack);
                LOGGER.debug("Fallback image name: {}", fallbackImageName);
                return Optional.of(fallbackImageName);
            } catch (ImageNotFoundException e) {
                LOGGER.warn("Fallback image could not be determined due to exception {}," +
                        " we should continue execution", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void performImageFallback(ImageEntity currentImage, Stack stack) {
        if (imageFallbackPermitted(currentImage, stack)) {
            ImageWrapper imageWrapper = getImageWrapper(currentImage, stack);
            String newImageName = imageService.determineImageNameByRegion(stack.getCloudPlatform(), stack.getRegion(), imageWrapper.getImage());
            currentImage.setImageName(newImageName);
            currentImage.setAccountId(stack.getAccountId());
            imageService.save(currentImage);
            LOGGER.info("Selected image to fallback to: {}", currentImage.getImageName());
        }
    }

    public ImageWrapper getImageWrapper(ImageEntity currentImage, Stack stack) {
        if (!OsType.vhdIsSupported(currentImage.getOsType()) && azureImageFormatValidator.isVhdImageFormat(currentImage.getImageName())) {
            String message = String.format("Failed to start instances with image: %s. The current image is a Redhat 8 VHD image, " +
                            "please check if the source image is signed: %s.",
                    currentImage.getImageName(), currentImage.getSourceImage());
            throw new CloudbreakServiceException(message);
        }

        ImageProvider imageProvider = imageProviderFactory.getImageProvider(currentImage.getImageCatalogName());
        FreeIpaImageFilterSettings imageFilterSettings = createFreeIpaImageFilterSettings(stack, currentImage);
        LOGGER.debug("Azure Marketplace image terms were not accepted. " +
                        "VHD image couldn't be found in image entry: '{}' for the selected platform: '{}' and region: '{}'. ",
                imageFilterSettings.catalog(), stack.getCloudPlatform(), stack.getRegion());
        String imgNotFoundMsg = String.format("Azure Marketplace image terms were not accepted. " +
                        "Attempted to fallback to VHD image, but failed. No VHD image found for image id %s and region %s.",
                currentImage.getImageId(), stack.getRegion());
        return imageProvider.getImage(imageFilterSettings)
                .orElseThrow(() -> new ImageNotFoundException(imgNotFoundMsg));
    }

    private FreeIpaImageFilterSettings createFreeIpaImageFilterSettings(Stack stack, ImageEntity currentImage) {
        return new FreeIpaImageFilterSettings(currentImage.getImageId(), currentImage.getImageCatalogName(), currentImage.getOs(), currentImage.getOs(),
                stack.getRegion(), stack.getCloudPlatform().toLowerCase(Locale.ROOT), false, stack.getArchitecture());
    }
}