package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.type.CommonResourceType;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpdateImageHandler implements CloudPlatformEventHandler<UpdateImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<UpdateImageRequest> type() {
        return UpdateImageRequest.class;
    }

    @Override
    public void accept(Event<UpdateImageRequest> event) {
        LOGGER.debug("Received event: {}", event);
        UpdateImageRequest<UpdateImageResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            CloudStack stack = request.getCloudStack();

            List<CloudResource> cloudResources = request.getCloudResources();
            cloudResources.stream().filter(resource -> resource.getType().getCommonResourceType() == CommonResourceType.TEMPLATE)
                    .forEach(resource -> resource.putParameter(CloudResource.IMAGE, stack.getImage().getImageName()));

            connector.resources().update(auth, stack, cloudResources);
            UpdateImageResult result = new UpdateImageResult(request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Update image finished for {}", cloudContext);
        } catch (Exception e) {
            UpdateImageResult failure = new UpdateImageResult(e.getMessage(), e, request);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
