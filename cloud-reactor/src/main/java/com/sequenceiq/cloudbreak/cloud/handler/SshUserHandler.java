package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class SshUserHandler implements CloudPlatformEventHandler<SshUserRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshUserHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<SshUserRequest> type() {
        return SshUserRequest.class;
    }

    @Override
    public void accept(Event<SshUserRequest> event) {
        LOGGER.info("Received event: {}", event);
        SshUserRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        String platform = cloudContext.getPlatform();
        CloudConnector connector = cloudPlatformConnectors.get(platform);
        request.getResult().onNext(new SshUserResponse(cloudContext, connector.sshUser()));
    }
}
