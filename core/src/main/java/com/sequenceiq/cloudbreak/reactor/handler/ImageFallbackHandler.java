package com.sequenceiq.cloudbreak.reactor.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ImageFallbackHandler extends ExceptionCatcherEventHandler<ImageFallbackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageFallbackHandler.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ImageFallbackService imageFallbackService;

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
        ImageFallbackRequest request = event.getData();
        Long stackId = request.getResourceId();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.azureOnlyMarketplaceImagesEnabled(accountId)) {
            return new ImageFallbackFailed(stackId, new CloudbreakServiceException("Cannot fallback to VHD image. Only Azure Marketplace images allowed."));
        }

        try {
            imageFallbackService.fallbackToVhd(stackId);
        } catch (Exception e) {
            LOGGER.error("Image fallback failed", e);
            return new ImageFallbackFailed(stackId, e);
        }

        return new ImageFallbackSuccess(stackId);
    }

}
