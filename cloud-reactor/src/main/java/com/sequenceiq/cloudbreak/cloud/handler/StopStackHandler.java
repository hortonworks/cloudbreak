package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class StopStackHandler implements CloudPlatformEventHandler<StopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<StopInstancesRequest> type() {
        return StopInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopInstancesRequest> event) {
        LOGGER.info("Received event: {}", event);
        StopInstancesRequest request = event.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getCloudContext(), request.getCloudCredential());
            connector.instances().stop(authenticatedContext, request.getCloudInstances());
            //TODO poll
            request.getResult().onNext(new StopInstancesResult(request.getCloudContext(), "Stack successfully stopped"));
        } catch (Exception e) {
            LOGGER.error("Failed to handle StopStackRequest.", e);
            request.getResult().onNext(new SetupResult(e));
        }
        LOGGER.info("StopStackHandler finished");
    }


}
