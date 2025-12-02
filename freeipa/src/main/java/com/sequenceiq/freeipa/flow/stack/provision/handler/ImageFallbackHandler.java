package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
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
    private ImageFallbackService imageFallbackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ImageFallbackRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ImageFallbackRequest> event) {
        return new ImageFallbackFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ImageFallbackRequest> event) {
        Long stackId = event.getData().getResourceId();
        Stack stack = stackService.getStackById(stackId);
        ImageEntity currentImage = imageService.getByStack(stack);
        if (!CloudPlatform.AZURE.name().equals(stack.getCloudPlatform())) {
            String msg = String.format("Failed to start instances with the designated image: %s. Image fallback is only supported on the Azure cloud platform",
                    currentImage.getImageName());
            LOGGER.warn(msg);
            return new ImageFallbackFailed(stackId, new CloudbreakServiceException(msg), ERROR);
        } else if (entitlementService.azureOnlyMarketplaceImagesEnabled(stack.getAccountId())) {
            String message = String.format("Azure Marketplace image terms were not accepted, cannot start instances with image: %s. " +
                            "Fallback to VHD image is not possible, only Azure Marketplace images allowed. " +
                            "Please accept image terms or turn on automatic image terms acceptance.",
                    currentImage.getImageName()
            );
            return new ImageFallbackFailed(stackId, new CloudbreakServiceException(message), ERROR);
        } else {
            try {
                imageFallbackService.performImageFallback(currentImage, stack);
                return new ImageFallbackSuccess(stackId);
            } catch (Exception e) {
                LOGGER.error("Image fallback failed", e);
                return new ImageFallbackFailed(stackId, e, ERROR);
            }
        }
    }
}
