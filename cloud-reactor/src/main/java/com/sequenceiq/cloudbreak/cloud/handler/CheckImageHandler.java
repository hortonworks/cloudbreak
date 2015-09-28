package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.common.type.ImageStatus;

import reactor.bus.Event;

@Component
public class CheckImageHandler implements CloudPlatformEventHandler<CheckImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;
    @Inject
    private PollTaskFactory statusCheckFactory;
    @Inject
    private SyncPollingScheduler<ResourceStatus> syncPollingScheduler;
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
            CloudStack cloudStack = request.getCloudStack();
            ImageStatusResult progress = connector.setup().checkImageStatus(auth, cloudStack);
            CheckImageResult imageResult = new CheckImageResult(request, progress.getImageStatus(), progress.getStatusProgressValue());
            request.getResult().onNext(imageResult);
            LOGGER.info("Provision setup finished for {}", cloudContext);
        } catch (Exception e) {
            request.getResult().onNext(new CheckImageResult(e, request, ImageStatus.CREATE_FAILED));
        }
    }
}
