package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageFallbackRequiredResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageFallbackException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class PrepareImageHandler implements CloudPlatformEventHandler<PrepareImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<PrepareImageRequest> type() {
        return PrepareImageRequest.class;
    }

    @Override
    public void accept(Event<PrepareImageRequest> event) {
        LOGGER.debug("Received event: {}", event);
        PrepareImageRequest<PrepareImageResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Image image = request.getImage();
            CloudStack stack = request.getStack();
            connector.setup().prepareImage(auth, stack, image, request.getPrepareImageType(), request.getImageFallbackTarget());
            PrepareImageResult result = new PrepareImageResult(request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Prepare image finished for {}", cloudContext);
        } catch (RuntimeException e) {
            LOGGER.debug("Should prepare fallback image: {}, fallback target exists: {}", e instanceof CloudImageFallbackException,
                    StringUtils.hasText(request.getImageFallbackTarget()));
            if (e instanceof CloudImageFallbackException && StringUtils.hasText(request.getImageFallbackTarget())) {
                LOGGER.info("Image fallback requested for {}", cloudContext.getName());
                PrepareImageFallbackRequiredResult result = new PrepareImageFallbackRequiredResult(request.getResourceId());
                request.getResult().onNext(result);
                eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            } else {
                LOGGER.info("Prepare image failed.", e);
                PrepareImageResult failure = new PrepareImageResult(e, request.getResourceId());
                request.getResult().onNext(failure);
                eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
            }
        }
    }
}
