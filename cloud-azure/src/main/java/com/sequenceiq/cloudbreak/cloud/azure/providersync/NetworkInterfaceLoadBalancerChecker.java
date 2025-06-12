package com.sequenceiq.cloudbreak.cloud.azure.providersync;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.network.fluent.models.BackendAddressPoolInner;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerOutboundRule;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NicIpConfiguration;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;

@Service
public class NetworkInterfaceLoadBalancerChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkInterfaceLoadBalancerChecker.class);

    /**
     * Checks if all provided network interfaces have outbound load balancer rules
     * with at least one common load balancer that provides outbound rules for ALL network interfaces
     */
    public NetworkInterfaceCheckResult checkNetworkInterfacesWithCommonLoadBalancer(List<String> networkInterfaceIds, AzureClient client) {
        if (networkInterfaceIds == null || networkInterfaceIds.isEmpty()) {
            return new NetworkInterfaceCheckResult("No NIC IDs provided", Collections.emptyMap());
        }

        try {
            List<NetworkInterface> networkInterfaces = getNetworkInterfaces(networkInterfaceIds, client);
            if (networkInterfaces.size() != networkInterfaceIds.size()) {
                return logIssueAndReturn("Could not retrieve all specified network interfaces");
            }

            List<LoadBalancer> loadBalancers = getLoadBalancers(client, getResourceGroup(networkInterfaceIds.getFirst()));
            return checkNetworkInterfacesInternal(networkInterfaces, loadBalancers);

        } catch (Exception e) {
            return logIssueAndReturn(e.getMessage());
        }
    }

    private List<LoadBalancer> getLoadBalancers(AzureClient client, String resourceGroup) {
        return client.getLoadBalancers(resourceGroup).getAll();
    }

    /**
     * Internal method to check network interfaces and find common load balancers
     * that provide outbound rules for all network interfaces.
     */

    private NetworkInterfaceCheckResult checkNetworkInterfacesInternal(List<NetworkInterface> networkInterfaces, List<LoadBalancer> loadBalancers) {
        Map<String, NetworkInterfaceAnalysis> analysisResults = analyzeAllNetworkInterfaces(networkInterfaces, loadBalancers);

        Set<String> commonOutboundLoadBalancers = findLoadBalancersProvidingOutboundForAll(analysisResults.values());
        boolean hasValidCommonLoadBalancer = !commonOutboundLoadBalancers.isEmpty();

        String message;
        if (hasValidCommonLoadBalancer) {
            message = String.format("Found %d load balancer(s) providing outbound rules for all network interfaces: %s",
                    commonOutboundLoadBalancers.size(),
                    String.join(", ", commonOutboundLoadBalancers));
        } else {
            message = provideFailureReason(analysisResults);
        }

        loadBalancers.removeIf(lb -> !commonOutboundLoadBalancers.contains(lb.id()));

        return new NetworkInterfaceCheckResult(message, analysisResults, Set.copyOf(loadBalancers));
    }

    private String provideFailureReason(Map<String, NetworkInterfaceAnalysis> analysisResults) {
        String message;
        Set<String> nicsWithoutOutbound = getNicsWithoutOutbound(analysisResults);

        if (!nicsWithoutOutbound.isEmpty()) {
            message = String.format("Network interfaces without outbound load balancer rules: %s",
                    String.join(", ", nicsWithoutOutbound));
        } else {
            message = "No common load balancer provides outbound rules for all network interfaces";
        }
        return message;
    }

    private Map<String, NetworkInterfaceAnalysis> analyzeAllNetworkInterfaces(List<NetworkInterface> networkInterfaces, List<LoadBalancer> loadBalancers) {
        Map<String, NetworkInterfaceAnalysis> analysisResults = new HashMap<>();

        for (NetworkInterface nic : networkInterfaces) {
            NetworkInterfaceAnalysis analysis = analyzeNetworkInterface(nic, loadBalancers);
            analysisResults.put(nic.id(), analysis);
        }
        return analysisResults;
    }

    private Set<String> getNicsWithoutOutbound(Map<String, NetworkInterfaceAnalysis> analysisResults) {
        return analysisResults.values().stream()
                .filter(analysis -> analysis.getLoadBalancersWithOutboundRules().isEmpty())
                .map(analysis -> analysis.getNetworkInterface().name())
                .collect(Collectors.toSet());
    }

    private List<NetworkInterface> getNetworkInterfaces(List<String> networkInterfaceIds, AzureClient client) {
        String resourceGroup = getResourceGroup(networkInterfaceIds.getFirst());
        List<NetworkInterface> networkInterfacesFromProvider =
                client.getNetworkInterfaceListByNames(resourceGroup,
                        networkInterfaceIds.stream()
                                .map(ResourceId::fromString)
                                .map(ResourceId::name)
                                .toList());
        LOGGER.debug("Retrieved {} network interfaces from provider with ids {}", networkInterfacesFromProvider.size(), networkInterfaceIds);
        return networkInterfacesFromProvider;
    }

    private String getResourceGroup(String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        return resourceId.resourceGroupName();
    }

    private NetworkInterfaceAnalysis analyzeNetworkInterface(NetworkInterface nic, List<LoadBalancer> loadBalancers) {
        Map<String, Set<String>> loadBalancerToBackendPools = new HashMap<>();

        // Collect all load balancer associations and their backend pools
        for (NicIpConfiguration ipConfig : nic.ipConfigurations().values()) {
            checkBackendPools(ipConfig, loadBalancerToBackendPools);
        }

        Set<String> loadBalancersWithOutboundRules = findLoadBalancersWithOutboundRulesForNic(
                nic, loadBalancerToBackendPools, loadBalancers);

        return new NetworkInterfaceAnalysis(
                nic,
                Set.copyOf(loadBalancerToBackendPools.keySet()),
                loadBalancersWithOutboundRules
        );
    }

    private void checkBackendPools(NicIpConfiguration ipConfig, Map<String, Set<String>> loadBalancerToBackendPools) {
        if (ipConfig.innerModel().loadBalancerBackendAddressPools() != null) {
            for (BackendAddressPoolInner backendPool : ipConfig.innerModel().loadBalancerBackendAddressPools()) {
                String loadBalancerId = extractLoadBalancerIdFromBackendPool(backendPool.id());
                if (loadBalancerId != null) {
                    loadBalancerToBackendPools.computeIfAbsent(loadBalancerId, k -> new HashSet<>())
                            .add(backendPool.id());
                }
            }
        }
    }

    private Set<String> findLoadBalancersWithOutboundRulesForNic(NetworkInterface nic,
            Map<String, Set<String>> loadBalancerToBackendPools, List<LoadBalancer> loadBalancers) {
        Set<String> result = new HashSet<>();

        for (String loadBalancerId : loadBalancerToBackendPools.keySet()) {
            loadBalancers.stream()
                    .filter(lb -> loadBalancerId.equals(lb.id()))
                    .filter(lb -> hasOutboundRulesForNetworkInterface(lb, loadBalancerToBackendPools.get(loadBalancerId)))
                    .map(LoadBalancer::id)
                    .forEach(result::add);
        }
        return result;
    }

    private boolean hasOutboundRulesForNetworkInterface(LoadBalancer loadBalancer, Set<String> nicBackendPools) {
        Map<String, LoadBalancerOutboundRule> outboundRules = loadBalancer.outboundRules();
        if (outboundRules == null || outboundRules.isEmpty()) {
            return false;
        }

        // Check if any outbound rule applies to the backend pools that this NIC is part of
        for (LoadBalancerOutboundRule outboundRule : outboundRules.values()) {
            if (outboundRule.innerModel().backendAddressPool() != null) {
                String ruleBackendPoolId = outboundRule.innerModel().backendAddressPool().id();

                if (nicBackendPools.contains(ruleBackendPoolId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds load balancers that provide outbound rules for all network interfaces
     * by intersecting the sets of load balancers from each analysis.
     */
    private Set<String> findLoadBalancersProvidingOutboundForAll(Collection<NetworkInterfaceAnalysis> analyses) {
        return analyses.stream()
                .map(NetworkInterfaceAnalysis::getLoadBalancersWithOutboundRules)
                .reduce(this::intersectLoadBalancers)
                .orElse(Collections.emptySet());
    }

    private Set<String> intersectLoadBalancers(Set<String> first, Set<String> second) {
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);
        return intersection;
    }

    private String extractLoadBalancerIdFromBackendPool(String backendPoolId) {
        if (backendPoolId != null && backendPoolId.contains("/loadBalancers/")) {
            int endIndex = backendPoolId.indexOf("/backendAddressPools/");
            if (endIndex > 0) {
                return backendPoolId.substring(0, endIndex);
            }
        }
        return null;
    }

    private NetworkInterfaceCheckResult logIssueAndReturn(String message) {
        LOGGER.warn("Error during network interface validation: {}", message);
        return new NetworkInterfaceCheckResult("Error during validation: " + message,
                Collections.emptyMap());
    }
}