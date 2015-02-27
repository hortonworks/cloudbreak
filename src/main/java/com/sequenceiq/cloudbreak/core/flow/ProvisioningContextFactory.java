package com.sequenceiq.cloudbreak.core.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class ProvisioningContextFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningContextFactory.class);

    private ProvisioningContextFactory() {
    }

    public static final ProvisioningContext createContext() {
        return new ProvisioningContext();
    }

    public static ProvisioningContext createProvisioningSetupContext(CloudPlatform cloudPlatform, Long stackId) {
        ProvisioningContext provisioningContext = createContext();
        provisioningContext.setCloudPlatform(cloudPlatform);
        provisioningContext.setStackId(stackId);
        return provisioningContext;
    }

    public static ProvisioningContext createProvisioningContext(CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) {
        ProvisioningContext provisioningContext = createProvisioningSetupContext(cloudPlatform, stackId);
        provisioningContext.getSetupProperties().putAll(setupProperties);
        provisioningContext.getUserDataParams().putAll(userDataParams);
        return provisioningContext;
    }

    public static ProvisioningContext createMetadataSetupContext(CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) {
        ProvisioningContext provisioningContext = createProvisioningSetupContext(cloudPlatform, stackId);
        provisioningContext.getSetupProperties().putAll(setupProperties);
        provisioningContext.getUserDataParams().putAll(userDataParams);
        return provisioningContext;
    }

}
