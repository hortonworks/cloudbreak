package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformEncryptionKeysRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformEncryptionKeysResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class GetPlatformEncryptionKeysHandler implements CloudPlatformEventHandler<GetPlatformEncryptionKeysRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetPlatformEncryptionKeysHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformEncryptionKeysRequest> type() {
        return GetPlatformEncryptionKeysRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformEncryptionKeysRequest> getPlatformEncryptionKeysRequest) {
        LOGGER.debug("Received event: {}", getPlatformEncryptionKeysRequest);
        GetPlatformEncryptionKeysRequest request = getPlatformEncryptionKeysRequest.getData();

        try {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                    Platform.platform(request.getExtendedCloudCredential().getCloudPlatform()),
                    Variant.variant(request.getVariant()));
            CloudEncryptionKeys encryptionKeys = cloudPlatformConnectors.get(cloudPlatformVariant)
                    .platformResources()
                    .encryptionKeys(request.getExtendedCloudCredential(), Region.region(request.getRegion()), request.getFilters());
            GetPlatformEncryptionKeysResult getPlatformEncryptionKeysResult = new GetPlatformEncryptionKeysResult(request.getResourceId(), encryptionKeys);
            request.getResult().onNext(getPlatformEncryptionKeysResult);
            LOGGER.debug("Query platform encryption keys types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetPlatformEncryptionKeysResult(e.getMessage(), e, request.getResourceId()));
        }
    }
}
