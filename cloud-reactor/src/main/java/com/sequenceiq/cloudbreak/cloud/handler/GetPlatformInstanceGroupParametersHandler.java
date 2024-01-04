package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformInstanceGroupParameterResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformInstanceGroupParametersHandler implements CloudPlatformEventHandler<GetPlatformInstanceGroupParameterRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformInstanceGroupParametersHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformInstanceGroupParameterRequest> type() {
        return GetPlatformInstanceGroupParameterRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformInstanceGroupParameterRequest> getPlatformInstanceGroupParameterRequestEvent) {
        LOGGER.debug("Received event: {}", getPlatformInstanceGroupParameterRequestEvent);
        GetPlatformInstanceGroupParameterRequest request = getPlatformInstanceGroupParameterRequestEvent.getData();

        try {
            CloudPlatformVariant variant =
                    new CloudPlatformVariant(platform(request.getExtendedCloudCredential().getCloudPlatform()), variant(request.getVariant()));
            Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponses = cloudPlatformConnectors.get(variant)
                    .parameters().collectInstanceGroupParameters(request.getInstanceGroupParameterRequest());
            GetPlatformInstanceGroupParameterResult getPlatformInstanceGroupParameterResult =
                    new GetPlatformInstanceGroupParameterResult(request.getResourceId(), instanceGroupParameterResponses);
            request.getResult().onNext(getPlatformInstanceGroupParameterResult);
            LOGGER.debug("Query platform instance group parameters finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformInstanceGroupParameterResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
