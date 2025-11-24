package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK_INTERFACE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerSku;
import com.azure.resourcemanager.network.models.LoadBalancerSkuName;
import com.azure.resourcemanager.network.models.LoadBalancerSkuType;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.SkuAttributes;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureLoadBalancerSyncer implements ProviderResourceSyncer<ResourceType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureLoadBalancerSyncer.class);

    @Inject
    private NetworkInterfaceLoadBalancerChecker networkInterfaceLoadBalancerChecker;

    @Inject
    private AzureOutboundManager azureOutboundManager;

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        return checkLoadBalancers(resources, client);
    }

    @Override
    public boolean shouldSync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        Set<CloudResource> loadBalancers = getResourcesByType(resources);
        boolean shouldSyncForOutbound = azureOutboundManager.shouldSyncForOutbound(resources);
        boolean shouldSyncForLoadBalancers = shouldSyncForLoadBalancers(loadBalancers);
        LOGGER.debug("Checking if we should sync load balancers. Load balancers found: {}, should sync for outbound: {}, should sync for load balancers: {}",
                loadBalancers, shouldSyncForOutbound, shouldSyncForLoadBalancers);
        return shouldSyncForOutbound || shouldSyncForLoadBalancers;

    }

    private boolean shouldSyncForLoadBalancers(Set<CloudResource> loadBalancers) {
        LOGGER.debug("Load balancer resources found: {}, checking SKU attributes", loadBalancers);
        return loadBalancers.stream().anyMatch(this::hasBasicSku);
    }

    private boolean hasBasicSku(CloudResource loadBalancer) {
        SkuAttributes skuAttributes = loadBalancer.getTypedAttributes(SkuAttributes.class, SkuAttributes::new);
        if (skuAttributes == null || skuAttributes.getSku() == null) {
            return true;
        }
        return LoadBalancerSkuName.BASIC.getValue()
                .equalsIgnoreCase(skuAttributes.getSku());
    }

    @Override
    public ResourceType getResourceType() {
        return AZURE_LOAD_BALANCER;
    }

    private List<CloudResourceStatus> checkLoadBalancers(List<CloudResource> resources, AzureClient client) {
        List<CloudResourceStatus> result = new ArrayList<>();
        Set<String> loadBalancerList = getResourceReferencesByType(resources);

        if (!CollectionUtils.isEmpty(loadBalancerList)) {
            List<LoadBalancer> existingLoadBalancers = processExistingLoadBalancers(resources, client, result, loadBalancerList);
            LOGGER.debug("Existing load balancers found: {}", existingLoadBalancers);
            if (azureOutboundManager.shouldSyncForOutbound(resources)) {
                processNetworkInterfaceSyncForOutbound(resources, client, result);
            }
        } else {
            processNetworkInterfaceSyncForOutbound(resources, client, result);
        }

        LOGGER.debug("Load balancer resources checked: {}", result);
        return result;
    }

    private List<LoadBalancer> processExistingLoadBalancers(List<CloudResource> resources, AzureClient client,
            List<CloudResourceStatus> result, Set<String> loadBalancerList) {
        LOGGER.debug("Checking Load Balancers: {}", loadBalancerList);
        List<LoadBalancer> loadBalancers = new ArrayList<>();
        loadBalancerList.stream()
                .filter(Predicate.not(String::isBlank))
                .findFirst()
                .ifPresentOrElse(loadBalancerResourceId -> {
                    loadBalancers.addAll(getLoadBalancersFromProvider(client, loadBalancerResourceId, loadBalancerList));
                    loadBalancers.forEach(loadBalancer -> syncLoadBalancerMetadata(resources, loadBalancer, result));
                }, () -> LOGGER.debug("No load balancer resource reference found for load balancers: {}", loadBalancerList));
        return loadBalancers;
    }

    private void processNetworkInterfaceSyncForOutbound(List<CloudResource> resources, AzureClient client, List<CloudResourceStatus> result) {
        LOGGER.debug("No existing outbound load balancer resources found, checking network interfaces for common outbound load balancers");
        Set<String> networkInterfaces = getResourceListByType(resources, AZURE_NETWORK_INTERFACE);

        if (CollectionUtils.isEmpty(networkInterfaces)) {
            LOGGER.debug("No existing network interface resources found, this should not happen");
            return;
        }

        NetworkInterfaceCheckResult networkInterfaceCheckResult = networkInterfaceLoadBalancerChecker
                .checkNetworkInterfacesWithCommonLoadBalancer(new ArrayList<>(networkInterfaces), client);
        Set<LoadBalancer> commonOutboundLoadBalancers = networkInterfaceCheckResult.getCommonOutboundLoadBalancers();

        if (!commonOutboundLoadBalancers.isEmpty()) {
            LOGGER.debug("Found common outbound load balancers: {}", commonOutboundLoadBalancers);
            Optional<CloudResource> networkResource = getResourceByType(resources, AZURE_NETWORK);
            networkResource.ifPresentOrElse(cloudResource -> {
                result.add(azureOutboundManager.updateNetworkOutbound(cloudResource, OutboundType.LOAD_BALANCER));
                commonOutboundLoadBalancers.forEach(loadBalancer ->
                        syncLoadBalancerMetadata(resources, loadBalancer, result));
            }, () -> LOGGER.debug("Network resource not found, cannot update outbound load balancer"));
        } else {
            LOGGER.debug("No common outbound load balancers found for network interfaces");
        }
    }

    private Set<String> getResourceListByType(List<CloudResource> resources, ResourceType resourceType) {
        return resources.stream()
                .filter(r -> r.getType() == resourceType)
                .map(CloudResource::getReference)
                .collect(Collectors.toSet());
    }

    private List<LoadBalancer> getLoadBalancersFromProvider(AzureClient client, String loadBalancerResourceId, Set<String> loadBalancerList) {
        ResourceId parsedId = ResourceId.fromString(loadBalancerResourceId);
        return client.getLoadBalancers(loadBalancerList, parsedId.resourceGroupName());
    }

    private void syncLoadBalancerMetadata(List<CloudResource> resources, LoadBalancer loadBalancer, List<CloudResourceStatus> result) {
        Optional<CloudResource> loadBalancerResource = resources.stream()
                .filter(r -> loadBalancer.id().equals(r.getReference()))
                .findFirst();
        Optional<LoadBalancerSkuName> sku = getLoadBalancerSkuName(loadBalancer);

        CloudResource cloudResource = loadBalancerResource.orElseGet(() -> {
            LOGGER.debug("Load balancer resource {} not found, creating new resource", loadBalancer.id());
            return CloudResource.builder()
                    .withType(AZURE_LOAD_BALANCER)
                    .withStatus(CREATED)
                    .withName(loadBalancer.name())
                    .withReference(loadBalancer.id())
                    .withGroup(loadBalancer.resourceGroupName())
                    .withPersistent(true)
                    .build();
        });
        syncAttributes(sku, cloudResource);
        addOrUpdateCloudResourceStatus(result, cloudResource);
    }

    private void addOrUpdateCloudResourceStatus(List<CloudResourceStatus> result, CloudResource cloudResource) {
        Optional<CloudResourceStatus> existingResource = result.stream()
                .filter(crs -> Objects.equals(crs.getCloudResource().getReference(), cloudResource.getReference()))
                .findFirst();

        if (existingResource.isPresent()) {
            // Update existing CloudResourceStatus with new CloudResource and status
            CloudResourceStatus existing = existingResource.get();
            CloudResourceStatus updated = new CloudResourceStatus(cloudResource, ResourceStatus.CREATED);
            int index = result.indexOf(existing);
            result.set(index, updated);

            LOGGER.debug("Updated existing CloudResourceStatus for resource: {}", cloudResource.getReference());
        } else {
            result.add(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED));
            LOGGER.debug("Added new CloudResourceStatus for resource: {}", cloudResource.getReference());
        }
    }

    private Optional<LoadBalancerSkuName> getLoadBalancerSkuName(LoadBalancer loadBalancer) {
        return Optional.ofNullable(loadBalancer.sku())
                .map(LoadBalancerSkuType::sku)
                .map(LoadBalancerSku::name);
    }

    private void syncAttributes(Optional<LoadBalancerSkuName> sku, CloudResource loadBalancerResource) {
        sku.ifPresentOrElse(s -> {
            SkuAttributes skuAttributes = new SkuAttributes();
            skuAttributes.setSku(s.getValue());
            loadBalancerResource.setTypedAttributes(skuAttributes);
        }, () -> LOGGER.debug("Load balancer SKU not found for load balancer resource: {}", loadBalancerResource));
    }

}