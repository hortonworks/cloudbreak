package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.ImageStatus;
import com.sequenceiq.common.api.type.ImageStatusResult;

@Component
public class CheckImageHandler implements CloudPlatformEventHandler<CheckImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CheckImageRequest> type() {
        return CheckImageRequest.class;
    }

    @Override
    public void accept(Event<CheckImageRequest> event) {
        LOGGER.debug("Received event: {}", event);
        CheckImageRequest<CheckImageResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Image image = request.getImage();
            CloudStack stack = request.getStack();
            ImageStatusResult progress = connector.setup().checkImageStatus(auth, stack, image);
            CheckImageResult imageResult = new CheckImageResult(request.getResourceId(), progress.getImageStatus(), progress.getStatusProgressValue());
            request.getResult().onNext(imageResult);
            LOGGER.debug("Provision setup finished for {}", cloudContext);
        } catch (RuntimeException e) {
            CheckImageResult failure = new CheckImageResult(e, request.getResourceId(), ImageStatus.CREATE_FAILED);
            request.getResult().onNext(failure);
        }
    }
}
