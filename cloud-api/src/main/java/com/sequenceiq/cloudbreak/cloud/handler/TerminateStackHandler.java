package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectorV2;
import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

import reactor.bus.Event;

@Component
public class TerminateStackHandler implements CloudPlatformEventHandler<TerminateStackRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;


    @Override
    public Class<TerminateStackRequest> type() {
        return TerminateStackRequest.class;
    }

    @Override
    public void accept(Event<TerminateStackRequest> terminateStackRequestEvent) {
        LOGGER.info("Received event: {}", terminateStackRequestEvent);
        TerminateStackRequest terminateStackRequest = terminateStackRequestEvent.getData();
        try {
            String platform = terminateStackRequest.getStackContext().getPlatform();
            CloudPlatformConnectorV2 connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(terminateStackRequest.getStackContext(), terminateStackRequest.getCloudCredential());
            throw new UnsupportedOperationException("Not yet implemented!");
        } catch (Exception e) {
            LOGGER.error("Failed to handle LaunchStackRequest: {}", e);
            terminateStackRequest.getResult().onNext(new LaunchStackResult(terminateStackRequest.getStackContext(),
                    ResourceStatus.FAILED, e.getMessage(), null));
        }
        LOGGER.info("TerminateStackHandler finished");

    }
}
