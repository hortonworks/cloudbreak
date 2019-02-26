package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class CheckPlatformVariantHandler implements CloudPlatformEventHandler<CheckPlatformVariantRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPlatformVariantHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CheckPlatformVariantRequest> type() {
        return CheckPlatformVariantRequest.class;
    }

    @Override
    public void accept(Event<CheckPlatformVariantRequest> defaultPlatformVariantRequest) {
        LOGGER.info("Received event: {}", defaultPlatformVariantRequest);
        CheckPlatformVariantRequest request = defaultPlatformVariantRequest.getData();
        try {
            CloudConnector<Object> cloudConnector = cloudPlatformConnectors.get(request.getCloudContext().getPlatform(), request.getCloudContext().getVariant());
            Variant defaultVariant = cloudConnector.variant();
            CheckPlatformVariantResult platformParameterResult = new CheckPlatformVariantResult(request, defaultVariant);
            request.getResult().onNext(platformParameterResult);
            LOGGER.info("Query platform variant finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new CheckPlatformVariantResult(e.getMessage(), e, request));
        }
    }
}
