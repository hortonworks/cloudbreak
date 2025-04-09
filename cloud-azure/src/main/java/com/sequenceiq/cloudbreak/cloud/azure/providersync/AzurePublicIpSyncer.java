package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PUBLIC_IP;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.azure.resourcemanager.network.models.PublicIPSkuType;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.network.models.PublicIpAddressSku;
import com.azure.resourcemanager.network.models.PublicIpAddressSkuName;
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
public class AzurePublicIpSyncer implements ProviderResourceSyncer<ResourceType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePublicIpSyncer.class);

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public ResourceType getResourceType() {
        return AZURE_PUBLIC_IP;
    }

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        return checkPublicIps(resources, client);
    }

    private List<CloudResourceStatus> checkPublicIps(List<CloudResource> resources, AzureClient client) {
        List<CloudResourceStatus> result = new ArrayList<>();
        Set<String> publicIpList = getResourceListByType(resources);
        if (!CollectionUtils.isEmpty(publicIpList)) {
            LOGGER.debug("Checking public IP resources: {}", publicIpList);
            publicIpList.stream().filter(Predicate.not(String::isBlank)).findFirst().ifPresentOrElse(publicIpResourceId -> {
                List<PublicIpAddress> publicIpAddresses = getPublicIpAddressesFromProvider(client, publicIpResourceId, publicIpList);
                publicIpAddresses.forEach(publicIpAddress -> syncPublicIpMetadata(resources, publicIpAddress, result));
            }, () -> LOGGER.debug("No public IP resource reference found for public IPs: {}", publicIpList));
        }
        LOGGER.debug("Public IP resources checked: {}", result);
        return result;
    }

    private List<PublicIpAddress> getPublicIpAddressesFromProvider(AzureClient client, String publicIpResourceId, Set<String> publicIpList) {
        ResourceId parsedId = ResourceId.fromString(publicIpResourceId);
        return client.getPublicIpAddresses(publicIpList, parsedId.resourceGroupName());
    }

    private void syncPublicIpMetadata(List<CloudResource> resources, PublicIpAddress publicIpAddress, List<CloudResourceStatus> result) {
        Optional<CloudResource> publicIpResource = resources.stream()
                .filter(r -> publicIpAddress.id().equals(r.getReference()))
                .findFirst();
        if (publicIpResource.isPresent()) {
            Optional<PublicIpAddressSkuName> sku = Optional.ofNullable(publicIpAddress.sku())
                    .map(PublicIPSkuType::sku)
                    .map(PublicIpAddressSku::name);
            syncAttributes(publicIpAddress, sku, publicIpResource.get());
            result.add(new CloudResourceStatus(publicIpResource.get(), ResourceStatus.CREATED));
        } else {
            LOGGER.debug("Public IP resource {} not found, this should not happen, " +
                    "please open a support ticket to fix the Management Console metadata!", publicIpAddress.id());
        }
    }

    private void syncAttributes(PublicIpAddress publicIpAddress, Optional<PublicIpAddressSkuName> sku, CloudResource publicIpResource) {
        SkuAttributes skuAttributes = new SkuAttributes();
        sku.ifPresentOrElse(s -> {
            skuAttributes.setSku(s.getValue());
            skuAttributes.setIpAllocationMethod(publicIpAddress.ipAllocationMethod().getValue());
            publicIpResource.setTypedAttributes(skuAttributes);
        }, () -> LOGGER.debug("Public IP SKU not found for public IP resource: {}", publicIpResource));
    }
}