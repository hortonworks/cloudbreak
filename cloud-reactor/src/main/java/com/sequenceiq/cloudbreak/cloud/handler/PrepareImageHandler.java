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
import com.sequenceiq.cloudbreak.cloud.model.Image;

import reactor.bus.Event;

@Component
public class PrepareImageHandler implements CloudPlatformEventHandler<PrepareImageRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareImageHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;
    @Override
    public Class<PrepareImageRequest> type() {
        return PrepareImageRequest.class;
    }

    @Override
    public void accept(Event<PrepareImageRequest> event) {
        LOGGER.info("Received event: {}", event);
        PrepareImageRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            Image image = request.getImage();
            connector.setup().prepareImage(auth, image);

            request.getResult().onNext(new PrepareImageResult(request));
            LOGGER.info("Prepare image finished for {}", cloudContext);
        } catch (Exception e) {
            request.getResult().onNext(new PrepareImageResult(e, request));
        }
    }
}
