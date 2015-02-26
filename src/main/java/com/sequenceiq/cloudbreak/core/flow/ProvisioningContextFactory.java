package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisioningContextFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningContextFactory.class);

    public static ProvisioningContext create(CloudPlatform cloudPlatform, Long stackId) {
        ProvisioningContext provisioningContext = new ProvisioningContext();
        provisioningContext.setCloudPlatform(cloudPlatform);
        provisioningContext.setStackId(stackId);
        return provisioningContext;
    }
}
