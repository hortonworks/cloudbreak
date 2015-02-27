package com.sequenceiq.cloudbreak.core.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisioningContextFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningContextFactory.class);

    private ProvisioningContextFactory() {
    }

    public static ProvisioningContext create(CloudPlatform cloudPlatform, Long stackId) {
        ProvisioningContext provisioningContext = new ProvisioningContext();
        provisioningContext.setCloudPlatform(cloudPlatform);
        provisioningContext.setStackId(stackId);
        return provisioningContext;
    }

    public static ProvisioningContext create(CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) {
        ProvisioningContext provisioningContext = create(cloudPlatform, stackId);
        provisioningContext.getSetupProperties().putAll(setupProperties);
        provisioningContext.getUserDataParams().putAll(userDataParams);
        return provisioningContext;
    }
}
