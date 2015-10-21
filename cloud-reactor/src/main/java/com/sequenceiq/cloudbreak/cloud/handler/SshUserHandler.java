package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

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
        CloudCredential cloudCredential = request.getCloudCredential();
        request.getResult().onNext(new SshUserResponse(cloudContext, cloudCredential.getLoginUserName()));
    }
}
