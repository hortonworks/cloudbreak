package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CheckImageHandler implements CloudPlatformEventHandler<CheckImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<CheckImageRequest> type() {
        return CheckImageRequest.class;
    }

    @Override
    public void accept(Event<CheckImageRequest> event) {
        LOGGER.info("Received event: {}", event);
        CheckImageRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Image image = request.getImage();
            CloudStack stack = request.getStack();
            ImageStatusResult progress = connector.setup().checkImageStatus(auth, stack, image);
            CheckImageResult imageResult = new CheckImageResult(request, progress.getImageStatus(), progress.getStatusProgressValue());
            request.getResult().onNext(imageResult);
            LOGGER.info("Provision setup finished for {}", cloudContext);
        } catch (RuntimeException e) {
            CheckImageResult failure = new CheckImageResult(e, request, ImageStatus.CREATE_FAILED);
            request.getResult().onNext(failure);
        }
    }
}
