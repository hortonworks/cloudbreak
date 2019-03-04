package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class PlatformParameterHandler implements CloudPlatformEventHandler<PlatformParameterRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformParameterHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<PlatformParameterRequest> type() {
        return PlatformParameterRequest.class;
    }

    @Override
    public void accept(Event<PlatformParameterRequest> platformParameterRequestEvent) {
        LOGGER.debug("Received event: {}", platformParameterRequestEvent);
        PlatformParameterRequest request = platformParameterRequestEvent.getData();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            PlatformParameters platformParameters = connector.parameters();

            PlatformParameterResult platformParameterResult = new PlatformParameterResult(request, platformParameters);
            request.getResult().onNext(platformParameterResult);
            LOGGER.debug("Query platform parameters finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new PlatformParameterResult(e.getMessage(), e, request));
        }
    }
}
