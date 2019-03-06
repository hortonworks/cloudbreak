package com.sequenceiq.cloudbreak.cloud.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse;

import reactor.bus.Event;

@Component
public class SshUserHandler implements CloudPlatformEventHandler<SshUserRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshUserHandler.class);

    @Override
    public Class<SshUserRequest> type() {
        return SshUserRequest.class;
    }

    @Override
    public void accept(Event<SshUserRequest> event) {
        LOGGER.debug("Received event: {}", event);
        SshUserRequest<SshUserResponse<?>> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        request.getResult().onNext(new SshUserResponse(cloudContext, request.getLoginUserName()));
    }
}
