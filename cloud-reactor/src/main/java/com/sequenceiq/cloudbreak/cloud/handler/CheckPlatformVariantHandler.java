package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CheckPlatformVariantResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

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
    public void accept(Event<CheckPlatformVariantRequest> defaultPlatformVariantRequestEvent) {
        LOGGER.info("Received event: {}", defaultPlatformVariantRequestEvent);
        CheckPlatformVariantRequest request = defaultPlatformVariantRequestEvent.getData();
        try {
            CloudConnector cc = cloudPlatformConnectors.get(request.getCloudContext().getPlatform(), request.getCloudContext().getVariant());
            String defaultVariant = cc.variant();
            CheckPlatformVariantResult platformParameterResult = new CheckPlatformVariantResult(request, defaultVariant);
            request.getResult().onNext(platformParameterResult);
            LOGGER.info("Query platform variant finished.");
        } catch (Exception e) {
            request.getResult().onNext(new CheckPlatformVariantResult(e.getMessage(), e, request));
        }
    }
}
