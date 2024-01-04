package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParametersResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class PlatformParametersHandler implements CloudPlatformEventHandler<PlatformParametersRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformParametersHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<PlatformParametersRequest> type() {
        return PlatformParametersRequest.class;
    }

    @Override
    public void accept(Event<PlatformParametersRequest> platformParameterRequestEvent) {
        LOGGER.debug("Received event: {}", platformParameterRequestEvent);
        PlatformParametersRequest request = platformParameterRequestEvent.getData();
        Map<Platform, PlatformParameters> platformParameters = new HashMap<>();
        try {
            for (Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                platformParameters.put(connector.getKey(), cloudPlatformConnectors.getDefault(connector.getKey()).parameters());
            }
            PlatformParametersResult platformParameterResult = new PlatformParametersResult(request.getResourceId(), platformParameters);
            request.getResult().onNext(platformParameterResult);
            LOGGER.debug("Query platform parameters finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new PlatformParametersResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
