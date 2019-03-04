package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
            CloudConnector<?> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Image image = request.getImage();
            CloudStack stack = request.getStack();
            connector.setup().prepareImage(auth, stack, image);
            PrepareImageResult result = new PrepareImageResult(request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Prepare image finished for {}", cloudContext);
        } catch (RuntimeException e) {
            PrepareImageResult failure = new PrepareImageResult(e, request);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
