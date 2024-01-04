package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformVariantsHandler implements CloudPlatformEventHandler<GetPlatformVariantsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformVariantsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformVariantsRequest> type() {
        return GetPlatformVariantsRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformVariantsRequest> getPlatformVariantsRequestEvent) {
        LOGGER.debug("Received event: {}", getPlatformVariantsRequestEvent);
        GetPlatformVariantsRequest request = getPlatformVariantsRequestEvent.getData();
        try {
            PlatformVariants pv = cloudPlatformConnectors.getPlatformVariants();
            GetPlatformVariantsResult platformVariantResult = new GetPlatformVariantsResult(request.getResourceId(), pv);
            request.getResult().onNext(platformVariantResult);
            LOGGER.debug("Query platform variant finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformVariantsResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
