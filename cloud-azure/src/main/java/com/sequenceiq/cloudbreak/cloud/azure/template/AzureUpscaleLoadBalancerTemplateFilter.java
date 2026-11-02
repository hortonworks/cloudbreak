package com.sequenceiq.cloudbreak.cloud.azure.template;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_UPSCALE_SKIP_EXISTING_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerChildCollection.BACKEND_ADDRESS_POOLS;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.ProvisioningState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.ArmResourceType;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerChildCollection;
import com.sequenceiq.cloudbreak.cloud.azure.loadbalancer.AzureLoadBalancerResourceNames;

/**
 * Keeps existing load balancers out of the upscale ARM template.
 * <p>
 * The upscale template contains the full load balancer only because the new NICs depend on it. Deploying it in incremental mode replaces
 * the child collections of the existing load balancer, so pools, rules and probes added outside Cloudbreak are deleted. The NICs join the
 * backend pools with a resourceId reference, which works without the load balancer being in the template, so a load balancer that already
 * exists in Azure with all of its Cloudbreak backend pools is removed from the template, together with its public IP and the NIC dependencies
 * on it. Any other case, or any error, keeps the rendered template as it is.
 * <p>
 * Only active if both the {@code cb.azure.upscale.skipExistingLoadBalancer} property is on and the account has the
 * {@code CDP_CB_AZURE_UPSCALE_SKIP_EXISTING_LOAD_BALANCER} entitlement.
 */
@Component
public class AzureUpscaleLoadBalancerTemplateFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUpscaleLoadBalancerTemplateFilter.class);

    // Reads the template like the Azure SDK's azure-json reader the deployment is submitted with: raw control characters in strings (e.g. a tab in
    // a user defined tag value) are accepted, and a duplicate key keeps its last value. Not JsonUtil's mapper, because it rejects duplicate keys,
    // which stored templates of old stacks have: one "id" per load balancer in a single loadBalancerBackendAddressPools entry (arm-with-marketplace.ftl)
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();

    private static final String WHITESPACE = "\\s+";

    private static final String RESOURCES = "resources";

    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String DEPENDS_ON = "dependsOn";

    private static final String PROPERTIES = "properties";

    @Value("${cb.azure.upscale.skipExistingLoadBalancer}")
    private boolean skipExistingLoadBalancer;

    @Inject
    private EntitlementService entitlementService;

    /**
     * Returns the names of the load balancers in the rendered upscale template that can be left out of the deployment: the load balancer exists
     * in Azure, its provisioning succeeded, and it has every backend pool of the rendered load balancer. Returns none if the property is off,
     * the account has no entitlement, or the check fails.
     */
    public Set<String> decide(String renderedTemplate, AzureClient client, String resourceGroupName, String accountId) {
        if (!skipExistingLoadBalancer) {
            LOGGER.info("Skipping existing load balancers on upscale is disabled, load balancers are deployed with the upscale template");
            return Set.of();
        } else {
            return decideForAccount(renderedTemplate, client, resourceGroupName, accountId);
        }
    }

    /**
     * Removes the given load balancers, their public IPs and the dependencies on them from the rendered template. Returns the rendered template
     * unchanged if there is nothing to remove, if the template can't be processed, or if the result still refers to a removed resource.
     */
    public String apply(String renderedTemplate, Set<String> skippableLoadBalancers) {
        if (skippableLoadBalancers.isEmpty()) {
            return renderedTemplate;
        } else {
            return removeLoadBalancers(renderedTemplate, skippableLoadBalancers);
        }
    }

    private Set<String> decideForAccount(String renderedTemplate, AzureClient client, String resourceGroupName, String accountId) {
        try {
            if (!entitlementService.isAzureUpscaleSkipExistingLoadBalancerEnabled(accountId)) {
                LOGGER.info("Account {} has no {} entitlement, load balancers are deployed with the upscale template", accountId,
                        CDP_CB_AZURE_UPSCALE_SKIP_EXISTING_LOAD_BALANCER);
                return Set.of();
            } else {
                return findSkippableLoadBalancers(renderedTemplate, client, resourceGroupName);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            LOGGER.warn("Could not check the existing load balancers, load balancers are deployed with the upscale template", e);
            return Set.of();
        }
    }

    private Set<String> findSkippableLoadBalancers(String renderedTemplate, AzureClient client, String resourceGroupName) throws JsonProcessingException {
        Set<String> skippableLoadBalancers = new HashSet<>();
        for (JsonNode loadBalancer : resourcesOfType(OBJECT_MAPPER.readTree(renderedTemplate), ArmResourceType.LOAD_BALANCER)) {
            String loadBalancerName = loadBalancer.path(NAME).asText();
            if (isSkippable(loadBalancer, loadBalancerName, client, resourceGroupName)) {
                skippableLoadBalancers.add(loadBalancerName);
            }
        }
        return Set.copyOf(skippableLoadBalancers);
    }

    private String removeLoadBalancers(String renderedTemplate, Set<String> skippableLoadBalancers) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(renderedTemplate);
            if (!(root.get(RESOURCES) instanceof ArrayNode resources)) {
                LOGGER.warn("The upscale template has no resources array, load balancers {} are deployed with the upscale template", skippableLoadBalancers);
                return renderedTemplate;
            } else {
                removeResources(resources, skippableLoadBalancers);
                removeDependencies(resources, skippableLoadBalancers);
                String filteredTemplate = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                return verifiedFilteredTemplate(renderedTemplate, filteredTemplate, resources, skippableLoadBalancers);
            }
        } catch (JsonProcessingException | RuntimeException e) {
            LOGGER.warn("Could not remove load balancers {} from the upscale template, they are deployed with the upscale template",
                    skippableLoadBalancers, e);
            return renderedTemplate;
        }
    }

    private String verifiedFilteredTemplate(String renderedTemplate, String filteredTemplate, ArrayNode resources, Set<String> skippableLoadBalancers) {
        Optional<String> remainingReference = findRemainingReference(resources, filteredTemplate, skippableLoadBalancers);
        if (remainingReference.isPresent()) {
            LOGGER.warn("The filtered upscale template still refers to a removed resource ({}), load balancers {} are deployed with the upscale template",
                    remainingReference.get(), skippableLoadBalancers);
            return renderedTemplate;
        } else {
            LOGGER.info("Load balancers {} exist in Azure, they and their public IPs are left out of the upscale template", skippableLoadBalancers);
            return filteredTemplate;
        }
    }

    private boolean isSkippable(JsonNode renderedLoadBalancer, String loadBalancerName, AzureClient client, String resourceGroupName) {
        Optional<LoadBalancer> liveLoadBalancer = client.getLoadBalancer(resourceGroupName, loadBalancerName);
        if (liveLoadBalancer.isEmpty()) {
            LOGGER.warn("Load balancer {} is missing in Azure, it is deployed with the upscale template", loadBalancerName);
            return false;
        } else {
            return isSkippable(renderedLoadBalancer, loadBalancerName, liveLoadBalancer.get());
        }
    }

    private boolean isSkippable(JsonNode renderedLoadBalancer, String loadBalancerName, LoadBalancer loadBalancer) {
        ProvisioningState provisioningState = loadBalancer.innerModel().provisioningState();
        if (!ProvisioningState.SUCCEEDED.equals(provisioningState)) {
            LOGGER.warn("Load balancer {} is in provisioning state {}, it is deployed with the upscale template", loadBalancerName, provisioningState);
            return false;
        } else {
            return hasAllBackendPools(renderedLoadBalancer, loadBalancerName, loadBalancer);
        }
    }

    private boolean hasAllBackendPools(JsonNode renderedLoadBalancer, String loadBalancerName, LoadBalancer loadBalancer) {
        // Only the pools are checked: a missing pool breaks the new NICs' reference, so the deployment would fail and roll back. Missing rules
        // or probes don't fail it, and are only logged. Checking them too would trigger the full redeploy, which wipes the entries added outside
        // Cloudbreak, in healthy cases: the rendered names come from the current model, while Azure keeps the last deployed ones (e.g. a load
        // balancer last deployed before the rule names got the protocol suffix has port-443-rule, the template renders port-443TCP-rule),
        // after model changes like a new health check port or target group port, and when a customer renamed or replaced a Cloudbreak rule.
        Set<String> missingPools = new TreeSet<>(childNames(renderedLoadBalancer, BACKEND_ADDRESS_POOLS.getArmProperty()));
        missingPools.removeAll(BACKEND_ADDRESS_POOLS.getLiveNames(loadBalancer));
        if (!missingPools.isEmpty()) {
            LOGGER.warn("Backend pools {} are missing on load balancer {}, it is deployed with the upscale template", missingPools, loadBalancerName);
            return false;
        } else {
            logDifferencesFromTemplate(renderedLoadBalancer, loadBalancerName, loadBalancer);
            return true;
        }
    }

    private void logDifferencesFromTemplate(JsonNode renderedLoadBalancer, String loadBalancerName, LoadBalancer loadBalancer) {
        try {
            Map<String, Set<String>> entriesNotInTemplate = new LinkedHashMap<>();
            Map<String, Set<String>> entriesMissingInAzure = new LinkedHashMap<>();
            for (AzureLoadBalancerChildCollection collection : AzureLoadBalancerChildCollection.values()) {
                Set<String> renderedNames = childNames(renderedLoadBalancer, collection.getArmProperty());
                Set<String> liveNames = collection.getLiveNames(loadBalancer);
                addDifference(entriesNotInTemplate, collection, liveNames, renderedNames);
                addDifference(entriesMissingInAzure, collection, renderedNames, liveNames);
            }
            if (entriesNotInTemplate.isEmpty()) {
                LOGGER.info("Load balancer {} exists in Azure with all of its backend pools, it is left out of the upscale template", loadBalancerName);
            } else {
                LOGGER.info("Load balancer {} exists in Azure with all of its backend pools, it is left out of the upscale template. "
                        + "Entries not managed by Cloudbreak are kept: {}", loadBalancerName, entriesNotInTemplate);
            }
            if (!entriesMissingInAzure.isEmpty()) {
                LOGGER.warn("Load balancer {} in Azure misses entries of the upscale template: {}. They are not recreated, "
                        + "because the load balancer is left out of the upscale template", loadBalancerName, entriesMissingInAzure);
            }
        } catch (RuntimeException e) {
            LOGGER.info("Could not compare load balancer {} with the upscale template, it is left out of the upscale template", loadBalancerName, e);
        }
    }

    private void addDifference(Map<String, Set<String>> differences, AzureLoadBalancerChildCollection collection, Set<String> names,
            Set<String> namesToRemove) {
        Set<String> difference = new TreeSet<>(names);
        difference.removeAll(namesToRemove);
        if (!difference.isEmpty()) {
            differences.put(collection.getArmProperty(), difference);
        }
    }

    private void removeResources(ArrayNode resources, Set<String> skippableLoadBalancers) {
        for (int i = resources.size() - 1; i >= 0; i--) {
            if (shouldRemoveResource(resources.get(i), skippableLoadBalancers)) {
                resources.remove(i);
            }
        }
    }

    private void removeDependencies(ArrayNode resources, Set<String> skippableLoadBalancers) {
        Set<String> removedDependencies = skippableLoadBalancers.stream()
                .map(loadBalancerName -> "[resourceId('" + ArmResourceType.LOAD_BALANCER.getType() + "','" + loadBalancerName + "')]")
                .collect(Collectors.toSet());
        for (JsonNode resource : resources) {
            if (resource.get(DEPENDS_ON) instanceof ArrayNode dependsOn) {
                for (int i = dependsOn.size() - 1; i >= 0; i--) {
                    if (removedDependencies.contains(dependsOn.get(i).asText().replaceAll(WHITESPACE, ""))) {
                        dependsOn.remove(i);
                    }
                }
            }
        }
    }

    /**
     * Deliberately broader than the removal: a false positive only keeps the rendered template, never produces an invalid deployment.
     * The NICs' backend pool references legitimately contain the load balancer name, so only dependsOn is checked for it.
     */
    private Optional<String> findRemainingReference(ArrayNode resources, String filteredTemplate, Set<String> skippableLoadBalancers) {
        for (JsonNode resource : resources) {
            if (shouldRemoveResource(resource, skippableLoadBalancers)) {
                return Optional.of("resource " + resource.path(NAME).asText());
            }
            for (JsonNode dependency : resource.path(DEPENDS_ON)) {
                if (skippableLoadBalancers.stream().anyMatch(loadBalancerName -> dependency.asText().contains(loadBalancerName))) {
                    return Optional.of("dependency " + dependency.asText() + " of " + resource.path(NAME).asText());
                }
            }
        }
        return skippableLoadBalancers.stream()
                .map(AzureLoadBalancerResourceNames::publicIpName)
                .filter(filteredTemplate::contains)
                .findFirst()
                .map(publicIpName -> "public IP " + publicIpName);
    }

    private boolean shouldRemoveResource(JsonNode resource, Set<String> skippableLoadBalancers) {
        String type = resource.path(TYPE).asText();
        String name = resource.path(NAME).asText();
        return (ArmResourceType.LOAD_BALANCER.matches(type) && skippableLoadBalancers.contains(name))
                || (ArmResourceType.PUBLIC_IP_ADDRESS.matches(type)
                    && skippableLoadBalancers.stream().anyMatch(lb -> AzureLoadBalancerResourceNames.publicIpName(lb).equals(name)));
    }

    private List<JsonNode> resourcesOfType(JsonNode template, ArmResourceType type) {
        return StreamSupport.stream(template.path(RESOURCES).spliterator(), false)
                .filter(resource -> type.matches(resource.path(TYPE).asText()))
                .toList();
    }

    private Set<String> childNames(JsonNode renderedLoadBalancer, String collection) {
        return StreamSupport.stream(renderedLoadBalancer.path(PROPERTIES).path(collection).spliterator(), false)
                .map(child -> child.path(NAME).asText())
                .collect(Collectors.toSet());
    }
}
