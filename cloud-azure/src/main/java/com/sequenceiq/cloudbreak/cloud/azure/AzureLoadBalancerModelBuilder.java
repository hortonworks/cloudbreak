package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureOutboundRule;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class AzureLoadBalancerModelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureLoadBalancerModelBuilder.class);

    private final CloudStack cloudStack;

    private final String stackName;

    public AzureLoadBalancerModelBuilder(CloudStack cloudStack, String stackName) {
        this.cloudStack = cloudStack;
        this.stackName = stackName;
    }

    /**
     * Returns a map containing {@code loadBalancers} and {@code loadBalancerMapping} keys.
     * The values in the returned map are based on the Cloud Stack and stack name provided at construction.
     * Gateway Private load balancer cannot be created in parallel to regular private load balancer, therefore
     * we need to remove it from the list, but a special logic is implemented in the FTL template to create extra frontend IP and load balancing rule
     *
     * @return A map containing load balancers and load balancer mappings
     */
    public Map<String, Object> buildModel() {
        List<AzureLoadBalancer> azureLoadBalancers = buildAzureLoadBalancerModel(cloudStack, stackName);
        boolean gatewayPrivateLbNeeded = azureLoadBalancers.stream().anyMatch(lb -> LoadBalancerType.GATEWAY_PRIVATE == lb.getType());
        List<AzureLoadBalancer> filteredAzureLoadBalancers = azureLoadBalancers.stream()
                .filter(lb -> LoadBalancerType.GATEWAY_PRIVATE != lb.getType()).collect(toList());

        Map<String, Collection<AzureLoadBalancer>> instanceGroupToLoadBalancers = createTargetInstanceGroupMapping(filteredAzureLoadBalancers);

        return Map.of("loadBalancers", filteredAzureLoadBalancers,
                "loadBalancerMapping", instanceGroupToLoadBalancers,
                "gatewayPrivateLbNeeded", gatewayPrivateLbNeeded);
    }

    /**
     * Create a model of Azure load balancers from stack information.
     *
     * @param cloudStack containing CloudLoadBalancers to base the AzureLoadBalancers off of
     * @param stackName  part of the load balancer name, usually a stack name
     * @return a list of models representing Azure load balancers
     */
    private List<AzureLoadBalancer> buildAzureLoadBalancerModel(CloudStack cloudStack, String stackName) {
        return cloudStack.getLoadBalancers().stream()
                .map(lb -> convertCloudLoadBalancerToAzureLoadBalancer(lb, stackName))
                .collect(toList());
    }

    private AzureLoadBalancer convertCloudLoadBalancerToAzureLoadBalancer(CloudLoadBalancer cloudLoadBalancer, String stackName) {
        Set<String> instanceGroupNames = collectInstanceGroupNames(cloudLoadBalancer);
        List<AzureLoadBalancingRule> rules = collectLoadBalancingRules(cloudLoadBalancer);
        List<AzureOutboundRule> outboundRules = collectOutboundRules(cloudLoadBalancer);
        return buildAzureLb(cloudLoadBalancer.getType(), cloudLoadBalancer.getSku(), instanceGroupNames, rules, outboundRules, stackName);
    }

    private Set<String> collectInstanceGroupNames(CloudLoadBalancer cloudLoadBalancer) {
        return cloudLoadBalancer.getPortToTargetGroupMapping().values().stream()
                .flatMap(Collection::stream)
                .map(Group::getName)
                .collect(Collectors.toSet());
    }

    private List<AzureLoadBalancingRule> collectLoadBalancingRules(CloudLoadBalancer cloudLoadBalancer) {
        if (!LoadBalancerType.OUTBOUND.equals(cloudLoadBalancer.getType())) {
            return cloudLoadBalancer.getPortToTargetGroupMapping()
                    .entrySet().stream()
                    .flatMap(entry -> {
                        TargetGroupPortPair pair = entry.getKey();
                        Group firstGroup = entry.getValue().stream().findFirst().orElse(null);
                        if (pair.getTrafficProtocol() != null && NetworkProtocol.TCP_UDP.equals(pair.getTrafficProtocol())) {
                            return Stream.of(
                                new AzureLoadBalancingRule(pair, NetworkProtocol.TCP, firstGroup),
                                new AzureLoadBalancingRule(pair, NetworkProtocol.UDP, firstGroup)
                            );
                        } else {
                            return Stream.of(new AzureLoadBalancingRule(pair, firstGroup));
                        }
                    })
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    private List<AzureOutboundRule> collectOutboundRules(CloudLoadBalancer cloudLoadBalancer) {
        if (LoadBalancerType.OUTBOUND.equals(cloudLoadBalancer.getType())) {
            return cloudLoadBalancer.getPortToTargetGroupMapping()
                    .values().stream()
                    .map(groups -> {
                        Group firstGroup = groups.stream().findFirst().orElse(null);
                        return new AzureOutboundRule(firstGroup);
                    })
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    private AzureLoadBalancer buildAzureLb(LoadBalancerType type, LoadBalancerSku sku, Set<String> instanceGroupNames,
            List<AzureLoadBalancingRule> rules, List<AzureOutboundRule> outboundRules, String stackName) {
        return new AzureLoadBalancer.Builder()
                .setType(type)
                .setLoadBalancerSku(sku)
                .setInstanceGroupNames(instanceGroupNames)
                .setRules(rules)
                .setOutboundRules(outboundRules)
                .setStackName(stackName)
                .createAzureLoadBalancer();

    }

    /**
     * Creates a mapping between the instances groups and the load balancers.
     * Each instance group is mapped to a list of all the load balancers that route to it.
     * <p>
     * While this information is available via the AzureLoadBalancer#getInstanceGroupName method, this map is used to simplify the ARM template
     * logic because it shows which load balancers are associated with which instances groups, without having to iterate
     * over the entire list of load balancers in the template.
     *
     * @return A map of instance group names to the load balancers associated with them
     */
    private Map<String, Collection<AzureLoadBalancer>> createTargetInstanceGroupMapping(List<AzureLoadBalancer> azureLoadBalancers) {
        ListMultimap<String, AzureLoadBalancer> mapping = MultimapBuilder.hashKeys().arrayListValues().build();
        for (AzureLoadBalancer lb : azureLoadBalancers) {
            for (String group : lb.getInstanceGroupNames()) {
                mapping.put(group, lb);
            }
        }
        Map<String, Collection<AzureLoadBalancer>> map = mapping.asMap();

        LOGGER.debug("InstanceGroup to LoadBalancer mapping result: {}", map);
        return map;
    }
}

