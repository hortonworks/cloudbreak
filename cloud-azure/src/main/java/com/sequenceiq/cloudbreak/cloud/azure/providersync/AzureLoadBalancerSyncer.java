package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_LOAD_BALANCER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

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
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureLoadBalancerSyncer implements ProviderResourceSyncer<ResourceType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureLoadBalancerSyncer.class);

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
    public ResourceType getResourceType() {
        return AZURE_LOAD_BALANCER;
    }

    private List<CloudResourceStatus> checkLoadBalancers(List<CloudResource> resources, AzureClient client) {
        List<CloudResourceStatus> result = new ArrayList<>();
        Set<String> loadBalancerList = getResourceListByType(resources);
        if (!CollectionUtils.isEmpty(loadBalancerList)) {
            LOGGER.debug("Checking Load Balancers: {}", loadBalancerList);
            loadBalancerList.stream().filter(Predicate.not(String::isBlank)).findFirst().ifPresentOrElse(loadBalancerResourceId -> {
                List<LoadBalancer> loadBalancerAddresses = getLoadBalancersFromProvider(client, loadBalancerResourceId, loadBalancerList);
                loadBalancerAddresses.forEach(loadBalancer -> syncLoadBalancerMetadata(resources, loadBalancer, result));
            }, () -> LOGGER.debug("No load balancer resource reference found for load balancers: {}", loadBalancerList));
        }
        LOGGER.debug("Load balancer resources checked: {}", result);
        return result;
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
        if (loadBalancerResource.isPresent()) {
            syncAttributes(sku, loadBalancerResource.get());
            result.add(new CloudResourceStatus(loadBalancerResource.get(), ResourceStatus.CREATED));
        } else {
            LOGGER.debug("Load balancer resource {} not found, this should not happen, " +
                    "please open a support ticket to fix the Management Console metadata!", loadBalancer.id());
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