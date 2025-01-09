package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.subnet.selector.AzureSubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTransientDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class AzureNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNetworkConnector.class);

    private static final String NETWORK_ID_KEY = "networkId";

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureNetworkTemplateBuilder azureNetworkTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureSubnetRequestProvider azureSubnetRequestProvider;

    @Inject
    private AzureSubnetSelectorService azureSubnetSelectorService;

    @Inject
    private AzureDnsZoneService azureDnsZoneService;

    @Inject
    private AzureNetworkLinkService azureNetworkLinkService;

    @Inject
    private AzureTransientDeploymentService azureTransientDeploymentService;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkRequest) {
        AzureClient azureClient = azureClientService.getClient(networkRequest.getCloudCredential());
        String region = networkRequest.getRegion().value();
        List<SubnetRequest> subnetRequests = azureSubnetRequestProvider.provide(
                region,
                Lists.newArrayList(networkRequest.getPublicSubnets()),
                Lists.newArrayList(networkRequest.getPrivateSubnets()),
                networkRequest.isPrivateSubnetEnabled());

        Deployment templateDeployment;
        ResourceGroup resourceGroup;
        try {
            resourceGroup = getOrCreateResourceGroup(azureClient, networkRequest);
            String template = azureNetworkTemplateBuilder.build(networkRequest, subnetRequests, resourceGroup.name());
            String parametersMapAsString = new Json(Map.of()).getValue();
            templateDeployment = azureClient.createTemplateDeployment(resourceGroup.name(), networkRequest.getStackName(), template, parametersMapAsString);
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Network template deployment provisioning");
        } catch (Exception e) {
            LOGGER.warn("Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning network %s: %s", networkRequest.getStackName(), e.getMessage()));
        }
        Map<String, Map> outputMap = (HashMap) templateDeployment.outputs();
        String networkName = cropId((String) outputMap.get(NETWORK_ID_KEY).get("value"));
        Set<CreatedSubnet> subnets = createSubnets(
                subnetRequests,
                outputMap,
                region);
        return new CreatedCloudNetwork(networkRequest.getStackName(), networkName, subnets,
                createProperties(resourceGroup.name(), networkRequest.getStackName()));
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        if (StringUtils.isEmpty(networkDeletionRequest.getResourceGroup())) {
            LOGGER.debug("The deletable network does not contain a valid resource group name, it is null or empty!");
            return;
        }
        if (networkDeletionRequest.isExisting()) {
            LOGGER.debug("The network was provided by the user, deleting nothing.");
            return;
        }

        if (networkDeletionRequest.isSingleResourceGroup()) {
            deleteResources(networkDeletionRequest);
        } else {
            deleteNetworkResourceGroup(networkDeletionRequest);
        }
    }

    private void deleteResources(NetworkDeletionRequest networkDeletionRequest) {
        if (StringUtils.isEmpty(networkDeletionRequest.getNetworkId())) {
            LOGGER.debug("The deletable network does not contain a valid network id, it is null or empty!");
            return;
        }
        try {
            LOGGER.debug("Deleting network id and deployment, preserving the resource group");
            AzureClient azureClient = azureClientService.getClient(networkDeletionRequest.getCloudCredential());
            String resourceGroupName = networkDeletionRequest.getResourceGroup();
            String stackName = networkDeletionRequest.getStackName();
            azureTransientDeploymentService.handleTransientDeployment(azureClient, resourceGroupName, stackName);
            azureClient.deleteNetworkInResourceGroup(resourceGroupName, networkDeletionRequest.getNetworkId());
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Network and template deployment deletion");
        }
    }

    private void deleteNetworkResourceGroup(NetworkDeletionRequest networkDeletionRequest) {
        try {
            LOGGER.debug("Deleting network resource group");
            AzureClient azureClient = azureClientService.getClient(networkDeletionRequest.getCloudCredential());
            if (resourceGroupExists(azureClient, networkDeletionRequest)) {
                azureClient.deleteTemplateDeployment(networkDeletionRequest.getResourceGroup(), networkDeletionRequest.getStackName());
                azureClient.deleteResourceGroup(networkDeletionRequest.getResourceGroup());
            }
        } catch (ManagementException e) {
            throw azureUtils.convertToCloudConnectorException(e, "Network resource group deletion");
        }
    }

    private ResourceGroup getOrCreateResourceGroup(AzureClient azureClient, NetworkCreationRequest networkRequest) {
        String region = networkRequest.getRegion().value();
        Map<String, String> tags = Collections.unmodifiableMap(networkRequest.getTags());
        String resourceGroupName = networkRequest.getResourceGroup();
        ResourceGroup resourceGroup;
        if (StringUtils.isNotEmpty(resourceGroupName)) {
            LOGGER.debug("Fetching existing resource group {}", resourceGroupName);
            resourceGroup = azureClient.getResourceGroup(resourceGroupName);
        } else {
            LOGGER.debug("Creating resource group {}", resourceGroupName);
            String resourceGroupNameForCreation = azureUtils.generateResourceNameByNameAndId(
                    String.format("%s-", networkRequest.getEnvName()),
                    UUID.randomUUID().toString());
            resourceGroup = azureClient.createResourceGroup(resourceGroupNameForCreation, region, tags);
        }
        return resourceGroup;
    }

    private boolean resourceGroupExists(AzureClient azureClient, NetworkDeletionRequest networkDeletionRequest) {
        if (azureClient.getResourceGroup(networkDeletionRequest.getResourceGroup()) == null) {
            LOGGER.debug("No resource group found on cloud provider (Azure) with name: \"{}\"", networkDeletionRequest.getResourceGroup());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        AzureClient azureClient = azureClientService.getClient(credential);
        String resourceGroupName = azureUtils.getCustomResourceGroupName(network);
        String networkId = azureUtils.getCustomNetworkId(network);
        com.azure.resourcemanager.network.models.Network networkByResourceGroup = azureClient.getNetworkByResourceGroup(resourceGroupName, networkId);
        if (networkByResourceGroup == null || networkByResourceGroup.addressSpaces().isEmpty()) {
            throw new BadRequestException(String.format("Network could not be fetched from Azure with Resource Group name: %s and VNET id: %s. " +
                            "Please make sure that the name of the VNET is correct and is present in the Resource Group specified.",
                    resourceGroupName, networkId));
        }
        List<String> networkCidrs = networkByResourceGroup.addressSpaces();
        if (networkCidrs.size() > 1) {
            LOGGER.info("More than one network CIDRs for Resource Group name: {} and network id: {}. We will use the first one: {}",
                    resourceGroupName, networkId, networkCidrs.getFirst());
        }
        return new NetworkCidr(networkCidrs.getFirst(), networkCidrs);
    }

    @Override
    public SubnetSelectionResult chooseSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        return azureSubnetSelectorService.select(subnetMetas, subnetSelectionParameters);
    }

    @Override
    public List<CloudResource> createProviderSpecificNetworkResources(NetworkResourcesCreationRequest request) {
        PrivateDatabaseVariant privateEndpointVariant = request.getPrivateEndpointVariant();
        if (privateEndpointVariant.isZoneManagedByCdp()) {
            LOGGER.debug("DNS zone is managed by CDP. Checking the presence of DNS Zones and Network links..");
            AzureClient azureClient = azureClientService.getClient(request.getCloudCredential());
            String resourceGroup = request.getResourceGroup();
            AuthenticatedContext authenticatedContext = new AuthenticatedContext(request.getCloudContext(), request.getCloudCredential());

            Map<String, String> tags = request.getTags();
            AzureNetworkView networkView = new AzureNetworkView();
            networkView.setExistingNetwork(request.isExistingNetwork());
            networkView.setNetworkId(request.getNetworkId());
            networkView.setResourceGroupName(request.getNetworkResourceGroup());
            Set<AzureManagedPrivateDnsZoneServiceType> servicesWithExistingDnsZone = getServicesWithExistingDnsZone(request);
            List<CloudResource> createdResources = new ArrayList<>(azureDnsZoneService.checkOrCreateDnsZones(
                    authenticatedContext, azureClient, networkView, resourceGroup, tags, servicesWithExistingDnsZone, privateEndpointVariant));
            createdResources.addAll(azureNetworkLinkService.checkOrCreateNetworkLinks(
                    authenticatedContext, azureClient, networkView, resourceGroup, tags, servicesWithExistingDnsZone, privateEndpointVariant));
            return createdResources;
        } else {
            LOGGER.debug("Private endpoints setting is {}, there is nothing to do.", privateEndpointVariant);
            return List.of();
        }
    }

    private Set<AzureManagedPrivateDnsZoneServiceType> getServicesWithExistingDnsZone(NetworkResourcesCreationRequest request) {
        return request.getServicesWithExistingPrivateDnsZone().stream()
                .map(AzureManagedPrivateDnsZoneServiceType::valueOf)
                .collect(Collectors.toSet());
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    private Set<CreatedSubnet> createSubnets(List<SubnetRequest> subnetRequests, Map<String, Map> outputMap, String region) {
        Set<CreatedSubnet> createdSubnets = new HashSet<>();
        for (SubnetRequest subnetRequest : subnetRequests) {
            if (outputMap.containsKey(SUBNET_ID + subnetRequest.getIndex())) {
                CreatedSubnet createdSubnet = new CreatedSubnet();
                createdSubnet.setSubnetId(cropId((String) outputMap.get(SUBNET_ID + subnetRequest.getIndex()).get("value")));
                if (!Strings.isNullOrEmpty(subnetRequest.getPrivateSubnetCidr())) {
                    createdSubnet.setCidr(subnetRequest.getPrivateSubnetCidr());
                } else {
                    createdSubnet.setCidr(subnetRequest.getPublicSubnetCidr());
                }
                createdSubnet.setAvailabilityZone(region);
                createdSubnet.setType(subnetRequest.getType());
                createdSubnets.add(createdSubnet);
            } else {
                throw new CloudConnectorException("Subnet could not be found in the Azure deployment output.");
            }
        }
        return createdSubnets;
    }

    private String cropId(String id) {
        String result = id;
        int loc = id.lastIndexOf("/");
        if (loc > 0) {
            result = id.substring(loc + 1);
        }
        return result;
    }

    private Map<String, Object> createProperties(String resourceGroupName, String stackName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(RESOURCE_GROUP_NAME, resourceGroupName);
        properties.put("stackName", stackName);
        return properties;
    }
}