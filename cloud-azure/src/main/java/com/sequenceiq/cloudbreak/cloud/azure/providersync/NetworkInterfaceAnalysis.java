package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import java.util.Set;

import com.azure.resourcemanager.network.models.NetworkInterface;

public class NetworkInterfaceAnalysis {

    private final NetworkInterface networkInterface;

    private final Set<String> allLoadBalancerIds;

    private final Set<String> loadBalancersWithOutboundRules;

    public NetworkInterfaceAnalysis(NetworkInterface networkInterface,
            Set<String> allLoadBalancerIds, Set<String> loadBalancersWithOutboundRules) {
        this.networkInterface = networkInterface;
        this.allLoadBalancerIds = allLoadBalancerIds;
        this.loadBalancersWithOutboundRules = loadBalancersWithOutboundRules;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public Set<String> getAllLoadBalancerIds() {
        return allLoadBalancerIds;
    }

    public Set<String> getLoadBalancersWithOutboundRules() {
        return loadBalancersWithOutboundRules;
    }

    @Override
    public String toString() {
        return String.format("NIC: %s (%s), Resource Group: %s, All LBs: %d, Outbound LBs: %d",
                networkInterface.name(), networkInterface.id(), networkInterface.resourceGroupName(),
                allLoadBalancerIds.size(), loadBalancersWithOutboundRules.size());
    }
}

