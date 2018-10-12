package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.ParametersValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class ParametersValidationHandler implements CloudPlatformEventHandler<ParametersValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<ParametersValidationRequest> type() {
        return ParametersValidationRequest.class;
    }

    @Override
    public void accept(Event<ParametersValidationRequest> requestEvent) {
        LOGGER.info("Received event: {}", requestEvent);
        ParametersValidationRequest request = requestEvent.getData();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            connector.setup().validateParameters(auth, request.getParameters());
            request.getResult().onNext(new ParametersValidationResult(request));
        } catch (Exception e) {
            request.getResult().onNext(new ParametersValidationResult(e.getMessage(), e, request));
        }
    }
}
