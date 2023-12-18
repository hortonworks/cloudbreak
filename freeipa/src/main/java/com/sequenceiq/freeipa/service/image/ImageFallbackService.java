package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.Locale;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;

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
            LOGGER.warn(msg);
            return false;
        }
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
        if (RHEL8.getOs().equalsIgnoreCase(currentImage.getOsType()) && azureImageFormatValidator.isVhdImageFormat(currentImage.getImageName())) {
            String message = String.format("Failed to start instances with image: %s. No valid fallback path from Redhat 8 VHD image.",
                    currentImage.getImageName());
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
                stack.getRegion(), stack.getCloudPlatform().toLowerCase(Locale.ROOT), false);
    }
}
