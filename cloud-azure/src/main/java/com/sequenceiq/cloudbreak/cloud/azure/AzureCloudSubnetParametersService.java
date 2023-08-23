package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.network.models.Delegation;
import com.azure.resourcemanager.network.models.VirtualNetworkPrivateEndpointNetworkPolicies;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Service
public class AzureCloudSubnetParametersService {

    public static final String PRIVATE_ENDPOINT_NETWORK_POLICIES = "privateEndpointNetworkPolicies";

    public static final String FLEXIBLE_SERVER_DELEGATED_SUBNET = "flexibleServerDelegatedSubnet";

    private static final String ENABLED = "enabled";

    private static final String DISABLED = "disabled";

    private static final String FLEXIBLE_SERVER = "Microsoft.DBforPostgreSQL/flexibleServers";

    public void addPrivateEndpointNetworkPolicies(CloudSubnet cloudSubnet, VirtualNetworkPrivateEndpointNetworkPolicies azurePrivateEndpointNetworkPolicies) {
        cloudSubnet.putParameter(PRIVATE_ENDPOINT_NETWORK_POLICIES,
                VirtualNetworkPrivateEndpointNetworkPolicies.DISABLED.equals(azurePrivateEndpointNetworkPolicies) ? DISABLED : ENABLED);
    }

    public boolean isPrivateEndpointNetworkPoliciesDisabled(CloudSubnet cloudSubnet) {
        return Optional.ofNullable(cloudSubnet.getStringParameter(PRIVATE_ENDPOINT_NETWORK_POLICIES))
                .map(DISABLED::equals)
                .orElse(false);
    }

    public void addFlexibleServerDelegatedSubnet(CloudSubnet cloudSubnet, List<Delegation> delegations) {
        Boolean flexibleServerDelegatedSubnet = CollectionUtils.isNotEmpty(delegations) ? delegations.stream()
                .map(Delegation::serviceName)
                .anyMatch(serviceName -> serviceName.equals(FLEXIBLE_SERVER)) : Boolean.FALSE;
        cloudSubnet.putParameter(FLEXIBLE_SERVER_DELEGATED_SUBNET, flexibleServerDelegatedSubnet);
    }

    public boolean isFlexibleServerDelegatedSubnet(CloudSubnet cloudSubnet) {
        return Optional.ofNullable(cloudSubnet.getParameter(FLEXIBLE_SERVER_DELEGATED_SUBNET, Boolean.class))
                .orElse(false);
    }
}
