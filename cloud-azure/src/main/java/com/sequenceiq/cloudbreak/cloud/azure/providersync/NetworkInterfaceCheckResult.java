package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.azure.resourcemanager.network.models.LoadBalancer;

public class NetworkInterfaceCheckResult {

    private final String message;

    private final Map<String, NetworkInterfaceAnalysis> networkInterfaceAnalyses;

    private final Set<LoadBalancer> commonOutboundLoadBalancers;

    public NetworkInterfaceCheckResult(String message, Map<String, NetworkInterfaceAnalysis> analyses) {
        this(message, analyses, Collections.emptySet());
    }

    public NetworkInterfaceCheckResult(String message, Map<String, NetworkInterfaceAnalysis> analyses,
            Set<LoadBalancer> commonOutboundLoadBalancers) {
        this.message = message;
        this.networkInterfaceAnalyses = analyses;
        this.commonOutboundLoadBalancers = commonOutboundLoadBalancers;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, NetworkInterfaceAnalysis> getNetworkInterfaceAnalyses() {
        return networkInterfaceAnalyses;
    }

    public Set<LoadBalancer> getCommonOutboundLoadBalancers() {
        return commonOutboundLoadBalancers;
    }
}