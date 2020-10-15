package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Service
public class AzureCloudSubnetParametersService {

    public static final String PRIVATE_ENDPOINT_NETWORK_POLICIES = "privateEndpointNetworkPolicies";

    private static final String ENABLED = "enabled";

    private static final String DISABLED = "disabled";

    public void addPrivateEndpointNetworkPolicies(CloudSubnet cloudSubnet, String state) {
        cloudSubnet.putParameter(PRIVATE_ENDPOINT_NETWORK_POLICIES, "Disabled".equalsIgnoreCase(state) ? DISABLED : ENABLED);
    }

    public boolean isPrivateEndpointNetworkPoliciesDisabled(CloudSubnet cloudSubnet) {
        return Optional.ofNullable(cloudSubnet.getStringParameter(PRIVATE_ENDPOINT_NETWORK_POLICIES))
                .map(DISABLED::equals)
                .orElse(false);
    }
}
