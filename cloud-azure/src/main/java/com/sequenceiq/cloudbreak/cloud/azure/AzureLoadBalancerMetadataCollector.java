package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.network.LoadBalancerBackend;
import com.microsoft.azure.management.network.LoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AzureLoadBalancerMetadataCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureLoadBalancerMetadataCollector.class);

    @Inject
    private AvailabilitySetNameService availabilitySetNameService;

    public Map<String, Object> getParameters(AuthenticatedContext ac, String resourceGroup, String loadBalancerName) {
        LOGGER.info("Parsing Azure load balancer parameters for load balancer {}", loadBalancerName);
        Map<String, Object> parameters = parseTargetGroupCloudParams(ac, resourceGroup, loadBalancerName);
        parameters.put(AzureLoadBalancerMetadataView.LOADBALANCER_NAME, loadBalancerName);
        return parameters;
    }

    private Map<String, Object> parseTargetGroupCloudParams(AuthenticatedContext ac, String resourceGroup, String loadBalancerName) {
        Map<String, Object> parameters = new HashMap<>();
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        Map<String, LoadBalancingRule> rules = azureClient.getLoadBalancerRules(resourceGroup, loadBalancerName);

        Map<Integer, String> portToAsMapping = rules.values().stream()
                .collect(Collectors.toMap(LoadBalancingRule::backendPort, rule -> generateAvailabilitySetName(ac, rule.backend())));
        LOGGER.debug("Found port to availability set mapping [{}] for load balancer {}", portToAsMapping, loadBalancerName);

        portToAsMapping.forEach((port, availabilitySetName) -> {
            // Validate an availability set with the expected name actually exists
            Optional<AvailabilitySet> availabilitySet = Optional.ofNullable(azureClient.getAvailabilitySet(resourceGroup, availabilitySetName));
            if (availabilitySet.isPresent()) {
                LOGGER.debug("Found availability set with name {}", availabilitySetName);
                parameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(port), availabilitySetName);
            } else {
                LOGGER.warn("Expected availability set with name {} in resource group {}, but unable to find one.",
                        availabilitySetName, resourceGroup);
                parameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(port), null);
            }
        });
        return parameters;
    }

    private String generateAvailabilitySetName(AuthenticatedContext ac, LoadBalancerBackend backend) {
        String stackName = ac.getCloudContext().getName();
        String groupName = backend.inner().name().replace("-pool", "");
        return availabilitySetNameService.generateName(stackName, groupName);
    }
}
