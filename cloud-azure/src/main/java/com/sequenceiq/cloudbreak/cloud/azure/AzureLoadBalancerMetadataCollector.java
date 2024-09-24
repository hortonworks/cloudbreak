package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureLoadBalancerMetadataView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;

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

        // rule name ending with special "gateway" suffix is happening _only_ in the case of requested GATEWAY_PRIVATE load balancer
        // and in that case the second rule is basically the copy of the first one, only the name differs. See arm-v2.ftl
        Map<Integer, String> portToAsMapping = rules.values().stream()
                .filter(r -> !r.name().endsWith("gateway"))
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
        String groupName = backend.innerModel().name().replace("-pool", "");
        return availabilitySetNameService.generateName(stackName, groupName);
    }
}
