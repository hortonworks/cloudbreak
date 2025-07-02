package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.common.api.type.CommonStatus.DETACHED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NAT_GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SUBNET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ExternalResourceAttributes;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureNatGatewaySyncer implements ProviderResourceSyncer<ResourceType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNatGatewaySyncer.class);

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

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
        return AZURE_NAT_GATEWAY;
    }

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        return checkNatGateways(resources, client);
    }

    private List<CloudResourceStatus> checkNatGateways(List<CloudResource> resources, AzureClient client) {
        List<CloudResourceStatus> result = new ArrayList<>();
        Optional<CloudResource> natGateway = getResourceByType(resources, AZURE_NAT_GATEWAY);
        Optional<CloudResource> network = getResourceByType(resources, AZURE_NETWORK);
        String resourceGroup = getNetworkResourceGroup(network);
        Optional<CloudResource> subnet = getResourceByType(resources, AZURE_SUBNET);

        if (network.isPresent() && StringUtils.isNotEmpty(resourceGroup) && subnet.isPresent()) {
            Subnet subnetProperties = client.getSubnetProperties(
                    resourceGroup,
                    network.get().getName(),
                    subnet.get().getName());
            if (subnetProperties != null) {
                LOGGER.debug("Subnet {} found", subnetProperties.id());
                searchForNatGateway(subnetProperties, natGateway, result);
            } else {
                LOGGER.info("Subnet {} not found, this should not happen", subnet.get().getReference());
            }

        } else {
            LOGGER.info("Network ({}), resource group ({}) or subnet ({}) was not found, sync is skipped",
                    network,
                    resourceGroup,
                    subnet);
        }
        LOGGER.debug("NAT Gateway sync result: {}", result);
        return result;
    }

    private void searchForNatGateway(Subnet subnetProperties, Optional<CloudResource> natGateway, List<CloudResourceStatus> result) {
        String natGatewayId = subnetProperties.natGatewayId();
        if (StringUtils.isNotEmpty(natGatewayId)) {
            ResourceId natGatewayResourceId = ResourceId.fromString(natGatewayId);
            if (natGateway.isPresent()) {
                CloudResource existingNatGateway = natGateway.get();
                String existingNatGatewayReference = existingNatGateway.getReference();
                buildResource(natGatewayResourceId, natGatewayId, result);
                LOGGER.debug("NAT Gateway (existing:{} / new:{}) refreshed for subnet {}",
                        existingNatGatewayReference,
                        natGatewayId,
                        subnetProperties.id());
            } else {
                buildResource(natGatewayResourceId, natGatewayId, result);
                LOGGER.debug("NAT Gateway {} found for subnet {}", natGatewayId,  subnetProperties.id());

            }
        } else if (natGateway.isPresent()) {
            CloudResource existingNatGateway = natGateway.get();
            existingNatGateway.setStatus(DETACHED);
            result.add(new CloudResourceStatus(existingNatGateway, ResourceStatus.DELETED));
            LOGGER.warn("NAT Gateway {} was deleted for subnet {}", existingNatGateway.getReference(), subnetProperties.id());

        } else {
            LOGGER.debug("NAT Gateway resource for subnet {} not found, nothing to do", subnetProperties.id());
        }
    }

    private String getNetworkResourceGroup(Optional<CloudResource> network) {
        return network.flatMap(cloudResource -> Optional.ofNullable(cloudResource.getParameter(CloudResource.ATTRIBUTES, NetworkAttributes.class))
                        .map(NetworkAttributes::getResourceGroupName))
                .orElse(null);
    }

    private Optional<CloudResource> getResourceByType(List<CloudResource> resources, ResourceType resourceType) {
        return resources.stream().filter(r -> r.getType() == resourceType).findFirst();
    }

    private void buildResource(ResourceId natGatewayResourceId, String natGatewayId, List<CloudResourceStatus> result) {
        CloudResource newNatGateway =
                azureCloudResourceService.buildCloudResource(natGatewayResourceId.name(), natGatewayId, AZURE_NAT_GATEWAY);
        newNatGateway.setTypedAttributes(new ExternalResourceAttributes());
        result.add(new CloudResourceStatus(newNatGateway, ResourceStatus.CREATED));
    }
}