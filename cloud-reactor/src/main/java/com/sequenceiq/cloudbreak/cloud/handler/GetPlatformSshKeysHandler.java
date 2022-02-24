package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSshKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformSshKeysResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

import reactor.bus.Event;

@Component
public class GetPlatformSshKeysHandler implements CloudPlatformEventHandler<GetPlatformSshKeysRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformSshKeysHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformSshKeysRequest> type() {
        return GetPlatformSshKeysRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformSshKeysRequest> getPlatformSshKeysRequest) {
        LOGGER.debug("Received event: {}", getPlatformSshKeysRequest);
        GetPlatformSshKeysRequest request = getPlatformSshKeysRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudSshKeys cloudSshKeys = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources().sshKeys(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformSshKeysResult getPlatformSshKeysResult = new GetPlatformSshKeysResult(request.getResourceId(), cloudSshKeys);
            request.getResult().onNext(getPlatformSshKeysResult);
            LOGGER.debug("Query platform networks types finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetPlatformSshKeysResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
