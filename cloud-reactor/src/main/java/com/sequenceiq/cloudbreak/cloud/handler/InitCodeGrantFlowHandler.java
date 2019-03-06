package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowResponse;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class InitCodeGrantFlowHandler implements CloudPlatformEventHandler<InitCodeGrantFlowRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitCodeGrantFlowHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<InitCodeGrantFlowRequest> type() {
        return InitCodeGrantFlowRequest.class;
    }

    @Override
    public void accept(Event<InitCodeGrantFlowRequest> initCodeGrantFlowRequestEvent) {
        LOGGER.info("Received event: {}", initCodeGrantFlowRequestEvent);
        InitCodeGrantFlowRequest request = initCodeGrantFlowRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.getDefault(cloudContext.getPlatform());
            Map<String, String> parameters = connector.credentials().initCodeGrantFlow(cloudContext, request.getCloudCredential());
            InitCodeGrantFlowResponse initCodeGrantFlowResponse = new InitCodeGrantFlowResponse(request, parameters);
            request.getResult().onNext(initCodeGrantFlowResponse);
            LOGGER.info("Authorization code grant flow has initialized successfully.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new InitCodeGrantFlowResponse(e.getMessage(), e, request));
        }
    }
}
