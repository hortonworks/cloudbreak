package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class StartStackHandler implements CloudPlatformEventHandler<StartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<StartInstancesRequest> type() {
        return StartInstancesRequest.class;
    }

    @Override
    public void accept(Event<StartInstancesRequest> event) {
        LOGGER.info("Received event: {}", event);
        StartInstancesRequest request = event.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getCloudContext(), request.getCloudCredential());
            connector.instances().start(authenticatedContext, request.getCloudInstances());
            //TODO poll
            request.getResult().onNext(new StartInstancesResult(request.getCloudContext(), "Stack successfully started"));
        } catch (Exception e) {
            LOGGER.error("Failed to handle StopStackRequest.", e);
            request.getResult().onNext(new SetupResult(e, request));
        }
        LOGGER.info("StopStackHandler finished");
    }


}
