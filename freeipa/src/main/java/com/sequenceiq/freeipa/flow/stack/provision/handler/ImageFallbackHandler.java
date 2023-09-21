package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.Locale;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.dto.ImageWrapper;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.image.FreeIpaImageFilterSettings;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
import com.sequenceiq.freeipa.service.image.ImageProvider;
import com.sequenceiq.freeipa.service.image.ImageProviderFactory;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ImageFallbackHandler extends ExceptionCatcherEventHandler<ImageFallbackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFallbackHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageProviderFactory imageProviderFactory;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ImageFallbackRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ImageFallbackRequest> event) {
        return new ImageFallbackFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ImageFallbackRequest> event) {
        Long stackId = event.getData().getResourceId();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Stack stack = stackService.getStackById(stackId);
        if (!CloudPlatform.AZURE.name().equals(stack.getCloudPlatform())) {
            String msg = "Image fallback is only supported on the Azure cloud platform";
            LOGGER.warn(msg);
            return new ImageFallbackFailed(stackId, new CloudbreakServiceException(msg));
        }
        if (entitlementService.azureOnlyMarketplaceImagesEnabled(accountId)) {
            return new ImageFallbackFailed(stackId, new CloudbreakServiceException("Cannot fallback to VHD image. Only Azure Marketplace images allowed."));
        }

        try {
            ImageEntity currentImage = imageService.getByStack(stack);

            if (RHEL8.getOs().equalsIgnoreCase(currentImage.getOsType()) && azureImageFormatValidator.isVhdImageFormat(currentImage.getImageName())) {
                throw new CloudbreakServiceException("No valid fallback path from redhat8 VHD image.");
            }

            ImageProvider imageProvider = imageProviderFactory.getImageProvider(currentImage.getImageCatalogName());
            FreeIpaImageFilterSettings imageFilterSettings = createFreeIpaImageFilterSettings(stack, currentImage);
            String imgNotFoundMsg = String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                    imageFilterSettings.catalog(), stack.getCloudPlatform(), stack.getRegion());
            ImageWrapper imageWrapper = imageProvider.getImage(imageFilterSettings)
                    .orElseThrow(() -> new ImageNotFoundException(imgNotFoundMsg));
            String newImageName = imageService.determineImageNameByRegion(stack.getCloudPlatform(), stack.getRegion(), imageWrapper.getImage());
            currentImage.setImageName(newImageName);
            currentImage.setAccountId(stack.getAccountId());
            imageService.save(currentImage);
            LOGGER.info("Selected image to fallback to: {}", currentImage);
        } catch (Exception e) {
            LOGGER.error("Image fallback failed", e);
            return new ImageFallbackFailed(stackId, e);
        }

        return new ImageFallbackSuccess(stackId);
    }

    private FreeIpaImageFilterSettings createFreeIpaImageFilterSettings(Stack stack, ImageEntity currentImage) {
        return new FreeIpaImageFilterSettings(currentImage.getImageId(), currentImage.getImageCatalogUrl(), currentImage.getOs(), currentImage.getOs(),
                stack.getRegion(), stack.getCloudPlatform().toLowerCase(Locale.ROOT), false);
    }

}
