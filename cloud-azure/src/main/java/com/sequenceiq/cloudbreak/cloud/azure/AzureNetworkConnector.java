package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.subnet.selector.AzureSubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Service
public class AzureNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNetworkConnector.class);

    private static final String NETWORK_ID_KEY = "networkId";

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String SUBNET_CIDR_KEY = "subnetCidr";

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

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkRequest) {
        AzureClient azureClient = azureClientService.getClient(networkRequest.getCloudCredential());
        List<SubnetRequest> subnetRequests = azureSubnetRequestProvider.provide(
                networkRequest.getRegion().value(),
                Lists.newArrayList(networkRequest.getPublicSubnetCidrs()),
                Lists.newArrayList(networkRequest.getPrivateSubnetCidrs()));
        String template = azureNetworkTemplateBuilder.build(networkRequest, subnetRequests);
        String envName = networkRequest.getEnvName();
        Deployment templateDeployment;
        ResourceGroup resourceGroup;
        try {
            Map<String, String> tags = Collections.emptyMap();
            resourceGroup = azureClient.createResourceGroup(envName, networkRequest.getRegion().value(), tags);
            templateDeployment = azureClient.createTemplateDeployment(resourceGroup.name(), networkRequest.getStackName(), template, "");
        } catch (CloudException e) {
            LOGGER.info("Provisioning error, cloud exception happened: ", e);
            if (e.body() != null && e.body().details() != null) {
                String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                throw new CloudConnectorException(String.format("Stack provisioning failed, status code %s, error message: %s, details: %s",
                        e.body().code(), e.body().message(), details));
            } else {
                throw new CloudConnectorException(String.format("Stack provisioning failed: '%s', please go to Azure Portal for detailed message", e));
            }
        } catch (Exception e) {
            LOGGER.warn("Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning stack %s: %s", networkRequest.getStackName(), e.getMessage()));
        }
        Map<String, Map> outputMap = (HashMap) templateDeployment.outputs();
        String networkName = cropId((String) outputMap.get(NETWORK_ID_KEY).get("value"));
        Set<CreatedSubnet> subnets = createSubnets(
                subnetRequests,
                outputMap,
                networkRequest.getRegion().value());
        return new CreatedCloudNetwork(networkRequest.getStackName(), networkName, subnets,
                createProperties(resourceGroup.name(), networkRequest.getStackName()));
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        if (!networkDeletionRequest.isExisting()) {
            try {
                AzureClient azureClient = azureClientService.getClient(networkDeletionRequest.getCloudCredential());
                if (isResourceGroupExists(azureClient, networkDeletionRequest)) {
                    azureClient.deleteTemplateDeployment(networkDeletionRequest.getResourceGroup(), networkDeletionRequest.getStackName());
                    azureClient.deleteResourceGroup(networkDeletionRequest.getResourceGroup());
                }
            } catch (CloudException e) {
                LOGGER.warn("Deletion error, cloud exception happened: ", e);
                if (e.body() != null && e.body().details() != null) {
                    String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                    throw new CloudConnectorException(String.format("Stack deletion failed, status code %s, error message: %s, details: %s",
                            e.body().code(), e.body().message(), details));
                } else {
                    throw new CloudConnectorException(String.format("Stack deletion failed: '%s', please go to Azure Portal for detailed message", e));
                }
            }
        }
    }

    private boolean isResourceGroupExists(AzureClient azureClient, NetworkDeletionRequest networkDeletionRequest) {
        if (StringUtils.isEmpty(networkDeletionRequest.getResourceGroup())) {
            LOGGER.debug("The deletable network does not contain a valid resource group name, it is null or empty!");
            return false;
        }
        if (azureClient.getResourceGroup(networkDeletionRequest.getResourceGroup()) == null) {
            LOGGER.debug("No resource group found on cloud provider (Azure) with name: \"{}\"", networkDeletionRequest.getResourceGroup());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getNetworkCidr(Network network, CloudCredential credential) {
        AzureClient azureClient = azureClientService.getClient(credential);
        String resourceGroupName = azureUtils.getCustomResourceGroupName(network);
        String networkId = azureUtils.getCustomNetworkId(network);
        com.microsoft.azure.management.network.Network networkByResourceGroup = azureClient.getNetworkByResourceGroup(resourceGroupName, networkId);
        if (networkByResourceGroup == null || networkByResourceGroup.addressSpaces().isEmpty()) {
            throw new BadRequestException(String.format("Network could not be fetch from Azure with resource group name: %s and network id: %s",
                    resourceGroupName, networkId));
        }
        List<String> networkCidrs = networkByResourceGroup.addressSpaces();
        if (networkCidrs.size() > 1) {
            LOGGER.info("More than one network cidrs for resource group name: {} and network id: {}. We will use the first one: {}",
                    resourceGroupName, networkId, networkCidrs.get(0));
        }
        return networkCidrs.get(0);
    }

    @Override
    public SubnetSelectionResult selectSubnets(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        return azureSubnetSelectorService.select(subnetMetas, subnetSelectionParameters);
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
            if (outputMap.containsKey(SUBNET_ID_KEY + subnetRequest.getIndex())) {
                CreatedSubnet createdSubnet = new CreatedSubnet();
                createdSubnet.setSubnetId(cropId((String) outputMap.get(SUBNET_ID_KEY + subnetRequest.getIndex()).get("value")));
                if (!Strings.isNullOrEmpty(subnetRequest.getPrivateSubnetCidr())) {
                    createdSubnet.setCidr(subnetRequest.getPrivateSubnetCidr());
                } else {
                    createdSubnet.setCidr(subnetRequest.getPublicSubnetCidr());
                }
                createdSubnet.setAvailabilityZone(region);
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
        properties.put("resourceGroupName", resourceGroupName);
        properties.put("stackName", stackName);
        return properties;
    }

}
