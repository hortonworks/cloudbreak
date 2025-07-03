package com.sequenceiq.cloudbreak.cloud.azure.providersync;

import static com.sequenceiq.cloudbreak.cloud.model.OutboundType.NOT_DEFINED;
import static com.sequenceiq.cloudbreak.cloud.model.OutboundType.USER_ASSIGNED_NATGATEWAY;
import static com.sequenceiq.common.api.type.CommonStatus.DETACHED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NAT_GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_NETWORK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_SUBNET;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import com.sequenceiq.cloudbreak.cloud.model.OutboundType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.provider.ProviderResourceSyncer;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureNatGatewaySyncer implements ProviderResourceSyncer<ResourceType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNatGatewaySyncer.class);

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

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
    public ResourceType getResourceType() {
        return AZURE_NAT_GATEWAY;
    }

    @Override
    public List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        AzureClient client = authenticatedContext.getParameter(AzureClient.class);
        return checkNatGateways(resources, client);
    }

    @Override
    public boolean shouldSync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        // we should sync if NAT Gateway is deleted, too
        return !getResourceReferencesByType(resources).isEmpty() ||
                azureOutboundManager.shouldSyncForOutbound(resources);
    }

    private List<CloudResourceStatus> checkNatGateways(List<CloudResource> resources, AzureClient client) {
        List<CloudResourceStatus> result = new ArrayList<>();
        ResourceContext context = buildResourceContext(resources);

        if (!context.isValid()) {
            logSkippedSync(context);
            return result;
        }

        Subnet subnetProperties = retrieveSubnetProperties(client, context);
        if (subnetProperties != null) {
            processNatGatewaySyncForOutbound(context, subnetProperties, result);
        } else {
            LOGGER.warn("Subnet {} not found, this should not happen", context.getSubnet().getReference());
        }

        LOGGER.debug("NAT Gateway sync result: {}", result);
        return result;
    }

    private ResourceContext buildResourceContext(List<CloudResource> resources) {
        Optional<CloudResource> natGatewayOpt = getResourceByType(resources, AZURE_NAT_GATEWAY);
        Optional<CloudResource> networkOpt = getResourceByType(resources, AZURE_NETWORK);
        Optional<CloudResource> subnetOpt = getResourceByType(resources, AZURE_SUBNET);
        String resourceGroup = getNetworkResourceGroup(networkOpt);

        return new ResourceContext(natGatewayOpt, networkOpt, subnetOpt, resourceGroup);
    }

    private void logSkippedSync(ResourceContext context) {
        LOGGER.info("Network ({}), resource group ({}) or subnet ({}) was not found, sync is skipped",
                context.getNetwork(), context.getResourceGroup(), context.getSubnet());
    }

    private Subnet retrieveSubnetProperties(AzureClient client, ResourceContext context) {
        return client.getSubnetProperties(
                context.getResourceGroup(),
                context.getNetwork().getName(),
                context.getSubnet().getName());
    }

    private void processNatGatewaySyncForOutbound(ResourceContext context, Subnet subnetProperties, List<CloudResourceStatus> result) {
        LOGGER.debug("Subnet {} found, searching for Nat Gateway", subnetProperties.id());
        CloudResourceStatus natGateway = searchForNatGateway(subnetProperties, context.getNatGatewayOpt());
        NullUtil.doIfNotNull(natGateway, ngw -> {
            result.add(ngw);
            result.add(updateOutbound(context, ngw.getStatus()));
        });
    }

    private CloudResourceStatus updateOutbound(ResourceContext context, ResourceStatus status) {
        return azureOutboundManager.updateNetworkOutbound(context.getNetwork(), getOutboundType(status));
    }

    private OutboundType getOutboundType(ResourceStatus status) {
        return status == ResourceStatus.DELETED ? NOT_DEFINED : USER_ASSIGNED_NATGATEWAY;
    }

    private CloudResourceStatus searchForNatGateway(Subnet subnetProperties, Optional<CloudResource> natGateway) {
        String natGatewayId = subnetProperties.natGatewayId();

        if (StringUtils.isNotEmpty(natGatewayId)) {
            return handleExistingNatGateway(natGatewayId, subnetProperties, natGateway);
        } else if (natGateway.isPresent()) {
            return handleDeletedNatGateway(natGateway.get(), subnetProperties);
        } else {
            LOGGER.debug("NAT Gateway resource for subnet {} not found, nothing to do", subnetProperties.id());
            return null;
        }
    }

    private CloudResourceStatus handleExistingNatGateway(String natGatewayId, Subnet subnetProperties, Optional<CloudResource> natGateway) {
        ResourceId natGatewayResourceId = ResourceId.fromString(natGatewayId);

        if (natGateway.isPresent()) {
            String existingReference = natGateway.get().getReference();
            LOGGER.debug("NAT Gateway (existing:{} / new:{}) refreshed for subnet {}",
                    existingReference, natGatewayId, subnetProperties.id());
        } else {
            LOGGER.debug("NAT Gateway {} found for subnet {}", natGatewayId, subnetProperties.id());
        }
        return buildResource(natGatewayResourceId, natGatewayId);
    }

    private CloudResourceStatus handleDeletedNatGateway(CloudResource existingNatGateway, Subnet subnetProperties) {
        existingNatGateway.setStatus(DETACHED);
        LOGGER.warn("NAT Gateway {} was deleted for subnet {}", existingNatGateway.getReference(), subnetProperties.id());
        return new CloudResourceStatus(existingNatGateway, ResourceStatus.DELETED);
    }

    private String getNetworkResourceGroup(Optional<CloudResource> network) {
        return network.map(net -> net.getTypedAttributes(NetworkAttributes.class, NetworkAttributes::new).getResourceGroupName())
                .orElse(null);
    }

    private CloudResourceStatus buildResource(ResourceId natGatewayResourceId, String natGatewayId) {
        CloudResource newNatGateway =
                azureCloudResourceService.buildCloudResource(natGatewayResourceId.name(), natGatewayId, AZURE_NAT_GATEWAY);
        newNatGateway.setTypedAttributes(new ExternalResourceAttributes());
        return new CloudResourceStatus(newNatGateway, ResourceStatus.CREATED);
    }

    private static class ResourceContext {

        private final Optional<CloudResource> natGatewayOpt;

        private final String resourceGroup;

        private final CloudResource network;

        private final CloudResource subnet;

        ResourceContext(Optional<CloudResource> natGatewayOpt, Optional<CloudResource> networkOpt,
                Optional<CloudResource> subnetOpt, String resourceGroup) {
            this.natGatewayOpt = natGatewayOpt;
            this.resourceGroup = resourceGroup;
            this.network = networkOpt.orElse(null);
            this.subnet = subnetOpt.orElse(null);
        }

        public Optional<CloudResource> getNatGatewayOpt() {
            return natGatewayOpt;
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        public CloudResource getNetwork() {
            return network;
        }

        public CloudResource getSubnet() {
            return subnet;
        }

        boolean isValid() {
            return Objects.nonNull(network) && StringUtils.isNotEmpty(resourceGroup) && Objects.nonNull(subnet);
        }
    }
}