package com.sequenceiq.cloudbreak.cloud.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformPrivateDnsZonesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformPrivateDnsZonesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformPrivateDnsZonesHandler implements CloudPlatformEventHandler<GetPlatformPrivateDnsZonesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformPrivateDnsZonesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformPrivateDnsZonesRequest> type() {
        return GetPlatformPrivateDnsZonesRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformPrivateDnsZonesRequest> event) {
        LOGGER.debug("Received event to retrieve private DNS zones: {}", event);
        GetPlatformPrivateDnsZonesRequest request = event.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudPrivateDnsZones privateDnsZones = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().privateDnsZones(request.getExtendedCloudCredential(), request.getFilters());
            GetPlatformPrivateDnsZonesResult result = new GetPlatformPrivateDnsZonesResult(request.getResourceId(), privateDnsZones);
            request.getResult().onNext(result);
            LOGGER.debug("Query platform private DNS zones finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformPrivateDnsZonesResult(e.getMessage(), e, request.getResourceId()));
        }
    }

}
