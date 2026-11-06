package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.network.models.LoadBalancerBackend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerResourceNames;
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
        Map<Integer, LoadBalancingRule> portToRule = rules.values().stream()
                .filter(r -> !AzureLoadBalancerResourceNames.isGatewayRule(r.name()))
                .filter(r -> hasBackendPool(r, loadBalancerName))
                // rules added outside Cloudbreak survive upscale, so the Cloudbreak rule wins if they share a backend port
                .collect(Collectors.toMap(
                        LoadBalancingRule::backendPort,
                        Function.identity(),
                        (existingRule, newRule) -> preferCloudbreakRule(existingRule, newRule, loadBalancerName)));
        Map<Integer, String> portToAsMapping = portToRule.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> generateAvailabilitySetName(ac, entry.getValue().backend())));
        LOGGER.debug("Found port to availability set mapping [{}] for load balancer {}", portToAsMapping, loadBalancerName);

        portToAsMapping.forEach((port, availabilitySetName) -> {
            // Validate an availability set with the expected name actually exists
            Optional<AvailabilitySet> availabilitySet = azureClient.getAvailabilitySet(resourceGroup, availabilitySetName);
            if (availabilitySet.isPresent()) {
                LOGGER.debug("Found availability set with name {}", availabilitySetName);
                parameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(port), availabilitySetName);
            } else {
                logMissingAvailabilitySet(portToRule.get(port), availabilitySetName, resourceGroup, loadBalancerName);
                parameters.put(AzureLoadBalancerMetadataView.getAvailabilitySetParam(port), null);
            }
        });
        return parameters;
    }

    private boolean hasBackendPool(LoadBalancingRule rule, String loadBalancerName) {
        boolean hasBackendPool = rule.backend() != null && rule.backend().innerModel() != null && rule.backend().innerModel().name() != null;
        if (!hasBackendPool) {
            LOGGER.info("Ignoring load balancing rule {} of load balancer {}, because it has no backend pool", rule.name(), loadBalancerName);
        }
        return hasBackendPool;
    }

    // a rule added outside Cloudbreak usually points to a pool that has no availability set, so that is expected and not worth a warning
    private void logMissingAvailabilitySet(LoadBalancingRule rule, String availabilitySetName, String resourceGroup, String loadBalancerName) {
        if (AzureLoadBalancerResourceNames.isCloudbreakRule(rule.name(), rule.backendPort())) {
            LOGGER.warn("Expected availability set with name {} in resource group {}, but unable to find one.", availabilitySetName, resourceGroup);
        } else {
            LOGGER.info("No availability set with name {} in resource group {} for load balancing rule {} of load balancer {}, "
                    + "the rule was not created by Cloudbreak", availabilitySetName, resourceGroup, rule.name(), loadBalancerName);
        }
    }

    private LoadBalancingRule preferCloudbreakRule(LoadBalancingRule existingRule, LoadBalancingRule newRule, String loadBalancerName) {
        LoadBalancingRule preferredRule = !AzureLoadBalancerResourceNames.isCloudbreakRule(existingRule.name(), existingRule.backendPort())
                && AzureLoadBalancerResourceNames.isCloudbreakRule(newRule.name(), newRule.backendPort()) ? newRule : existingRule;
        LOGGER.info("Load balancing rules {} and {} of load balancer {} use the same backend port {}, using {}",
                existingRule.name(), newRule.name(), loadBalancerName, existingRule.backendPort(), preferredRule.name());
        return preferredRule;
    }

    private String generateAvailabilitySetName(AuthenticatedContext ac, LoadBalancerBackend backend) {
        String stackName = ac.getCloudContext().getName();
        String groupName = AzureLoadBalancerResourceNames.groupName(backend.innerModel().name());
        return availabilitySetNameService.generateName(stackName, groupName);
    }
}
