package com.sequenceiq.cloudbreak.reactor.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateImageRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateImageResult;

@Component
public class ValidateImageHandler implements CloudPlatformEventHandler<ValidateImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<ValidateImageRequest> type() {
        return ValidateImageRequest.class;
    }

    @Override
    public void accept(Event<ValidateImageRequest> event) {
        LOGGER.debug("Received event: {}", event);
        ValidateImageRequest<ValidateImageResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            CloudStack stack = request.getStack();

            String currentImageId = stack.getImage().getImageId();
            String requestImageId = request.getImage().getImageId();
            if (currentImageId.equalsIgnoreCase(requestImageId)) {
                LOGGER.info("Current image id [{}] is the same as in the request [{}]. Skipping validation as it is not necessary.", currentImageId,
                        requestImageId);
            } else {
                LOGGER.debug("Current image id [{}] is different from the one in the request [{}]. Calling image validation", currentImageId, requestImageId);
                connector.setup().validateImage(auth, stack, request.getImage());
            }

            ValidateImageResult result = new ValidateImageResult(request.getResourceId(), request.getStatedImage());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Validate image finished for {}", cloudContext);
        } catch (RuntimeException e) {
            ValidateImageResult failure = new ValidateImageResult(e, request.getResourceId());
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
