package com.sequenceiq.cloudbreak.cloud.azure.loadbalancer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.azure.resourcemanager.network.fluent.models.ProbeInner;
import com.azure.resourcemanager.network.models.LoadBalancer;

/**
 * The child collections of an Azure load balancer: the property name in the ARM template, paired with the names of the same entries on the
 * load balancer in Azure.
 */
public enum AzureLoadBalancerChildCollection {

    BACKEND_ADDRESS_POOLS("backendAddressPools", loadBalancer -> loadBalancer.backends().keySet()),
    LOAD_BALANCING_RULES("loadBalancingRules", loadBalancer -> loadBalancer.loadBalancingRules().keySet()),
    PROBES("probes", AzureLoadBalancerChildCollection::probeNames),
    FRONTEND_IP_CONFIGURATIONS("frontendIPConfigurations", loadBalancer -> loadBalancer.frontends().keySet()),
    OUTBOUND_RULES("outboundRules", loadBalancer -> loadBalancer.outboundRules().keySet()),
    INBOUND_NAT_RULES("inboundNatRules", loadBalancer -> loadBalancer.inboundNatRules().keySet());

    private final String armProperty;

    private final Function<LoadBalancer, Set<String>> liveNames;

    AzureLoadBalancerChildCollection(String armProperty, Function<LoadBalancer, Set<String>> liveNames) {
        this.armProperty = armProperty;
        this.liveNames = liveNames;
    }

    public String getArmProperty() {
        return armProperty;
    }

    public Set<String> getLiveNames(LoadBalancer loadBalancer) {
        return liveNames.apply(loadBalancer);
    }

    // probes() of the inner model contains every protocol, unlike tcpProbes(), httpProbes() and httpsProbes()
    private static Set<String> probeNames(LoadBalancer loadBalancer) {
        List<ProbeInner> probes = loadBalancer.innerModel().probes();
        return probes == null ? Set.of() : probes.stream().map(ProbeInner::name).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
