package com.sequenceiq.cloudbreak.core.flow;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

// todo make this class a builder
public class ProvisioningContextFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningContextFactory.class);

    private ProvisioningContextFactory() {
    }

    public static final ProvisioningContext createContext() {
        return new ProvisioningContext();
    }

    public static final ProvisioningContext createProvisioningSetupContext(CloudPlatform cloudPlatform, Long stackId) {
        ProvisioningContext provisioningContext = createContext();
        provisioningContext.setCloudPlatform(cloudPlatform);
        provisioningContext.setStackId(stackId);
        return provisioningContext;
    }

    public static final ProvisioningContext createProvisioningContext(CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) {
        ProvisioningContext provisioningContext = createProvisioningSetupContext(cloudPlatform, stackId);
        provisioningContext.getSetupProperties().putAll(setupProperties);
        provisioningContext.getUserDataParams().putAll(userDataParams);
        return provisioningContext;
    }

    public static final ProvisioningContext createMetadataSetupContext(CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) {
        ProvisioningContext provisioningContext = createProvisioningSetupContext(cloudPlatform, stackId);
        provisioningContext.getSetupProperties().putAll(setupProperties);
        provisioningContext.getUserDataParams().putAll(userDataParams);
        return provisioningContext;
    }

    public static final ProvisioningContext createAmbariStartContext(Long stackId, String ambariIp) {
        ProvisioningContext provisioningContext = createContext();
        provisioningContext.setStackId(stackId);
        provisioningContext.setAmbariIp(ambariIp);
        return provisioningContext;
    }

    public static final ProvisioningContext createClusterCreateSuccessContext(Long clusterId, long clusterCreationTime, String ambariIp) {
        ProvisioningContext context = createContext();
        context.setClusterId(clusterId);
        context.setClusterCreationTime(clusterCreationTime);
        context.setAmbariIp(ambariIp);
        return context;
    }

    public static final ProvisioningContext createClusterCreateFailureContext(Long stackId, long clusterId, String message) {
        ProvisioningContext context = createContext();
        context.setClusterId(clusterId);
        context.setStackId(stackId);
        context.setMessage(message);
        return context;
    }

    public static final ProvisioningContext createAmbariHostsUpdatedsSuccessContext(Long clusterId, Set<String> hostNames, boolean decommission) {
        ProvisioningContext context = createContext();
        context.getHostNames().addAll(hostNames);
        context.setClusterId(clusterId);
        context.setDecommision(decommission);
        return context;
    }

    public static final ProvisioningContext createAmbariHostsUpdatedsFailureContext(Long clusterId, String message, boolean decommission) {
        ProvisioningContext context = createContext();
        context.setMessage(message);
        context.setClusterId(clusterId);
        context.setDecommision(decommission);
        return context;
    }

}
