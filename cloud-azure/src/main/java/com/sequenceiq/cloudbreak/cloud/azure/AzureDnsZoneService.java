package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VIRTUAL_NETWORK_LINK;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationPoller;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureDnsZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDnsZoneService.class);

    private static final int DEPLOYMENT_LENGTH_LIMIT = 64;

    private static final String DNS_ZONES = "-dns-zones";

    private static final String NETWORK_LINKS = "-links";

    @Inject
    private AzureNetworkDnsZoneTemplateBuilder azureNetworkDnsZoneTemplateBuilder;

    @Inject
    private PersistenceRetriever resourcePersistenceRetriever;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private AzureDnsZoneCreationPoller azureDnsZoneCreationPoller;

    @Inject
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Inject
    private AzureUtils azureUtils;

    @Value("${cb.arm.privateendpoint.services:}")
    private List<String> privateEndpointServices;

    public void getOrCreateDnsZones(AuthenticatedContext authenticatedContext, AzureClient azureClient, AzureNetworkView networkView,
            String resourceGroup, Map<String, String> tags) {

        String networkId = networkView.getNetworkId();
        String networkResourceGroup = networkView.getResourceGroupName();
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = getEnabledPrivateEndpointServices();

        boolean dnsZonesDeployed = azureClient.checkIfDnsZonesDeployed(resourceGroup, enabledPrivateEndpointServices);
        boolean networkLinksDeployed = azureClient.checkIfNetworkLinksDeployed(resourceGroup, networkId, enabledPrivateEndpointServices);
        String deploymentName = generateDeploymentName(enabledPrivateEndpointServices, DNS_ZONES);
        String dnsZoneDeploymentId = azureResourceIdProviderService.generateDeploymentId(azureClient.getCurrentSubscription().subscriptionId(),
                resourceGroup, deploymentName);

        if (dnsZonesDeployed && networkLinksDeployed) {
            LOGGER.debug("Dns zones ({}) and network links already deployed in resource group {}", enabledPrivateEndpointServices, resourceGroup);
        } else if (!dnsZonesDeployed) {
            LOGGER.debug("Dns zones are not deployed yet!");
            String azureNetworkId = getAzureNetwork(azureClient, networkId, networkResourceGroup).id();
            try {
                if (isRequested(dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE)) {
                    LOGGER.debug("Dns zones ({}) already requested in resource group {}", enabledPrivateEndpointServices, resourceGroup);
                    pollForCreation(authenticatedContext, azureClient, resourceGroup, deploymentName, dnsZoneDeploymentId,
                            enabledPrivateEndpointServices, null);
                } else {
                    LOGGER.debug("Dns zones ({}) are not requested yet in resource group {}", enabledPrivateEndpointServices, resourceGroup);

                    if (isCreated(dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE)) {
                        LOGGER.debug("Dns zone deployment ({}) is there in database but not deployed on Azure, resetting it..", dnsZoneDeploymentId);
                        updateCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId, CommonStatus.REQUESTED, AZURE_PRIVATE_DNS_ZONE);
                    } else {
                        persistCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE);
                    }

                    createDnsZonesAndNetworkLinks(azureClient, azureNetworkId, resourceGroup, tags, enabledPrivateEndpointServices);
                    updateCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId, CommonStatus.CREATED, AZURE_PRIVATE_DNS_ZONE);

                }
            } catch (CloudException | DataAccessException e) {
                LOGGER.warn("Deployment {} failed due to {}", deploymentName, e.getMessage());
                pollForCreation(authenticatedContext, azureClient, resourceGroup, deploymentName, dnsZoneDeploymentId,
                        enabledPrivateEndpointServices, null);
            }
        }
    }

    public void getOrCreateNetworkLinks(AuthenticatedContext authenticatedContext, AzureClient azureClient, AzureNetworkView networkView,
            String resourceGroup, Map<String, String> tags) {

        String networkId = networkView.getNetworkId();
        String networkResourceGroup = networkView.getResourceGroupName();
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = getEnabledPrivateEndpointServices();

        boolean networkLinksDeployed = azureClient.checkIfNetworkLinksDeployed(resourceGroup, networkId, enabledPrivateEndpointServices);
        String deploymentName = generateDeploymentName(enabledPrivateEndpointServices, "-" + networkId + NETWORK_LINKS);
        String networkLinkDeploymentId = azureResourceIdProviderService.generateDeploymentId(azureClient.getCurrentSubscription().subscriptionId(),
                resourceGroup, deploymentName);

        if (!networkLinksDeployed) {
            LOGGER.debug("Deploying network links that are not deployed yet!");
            String azureNetworkId = getAzureNetwork(azureClient, networkId, networkResourceGroup).id();

            try {
                if (isRequested(networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK)) {
                    LOGGER.debug("Network links ({}) already requested in resource group {}", enabledPrivateEndpointServices, resourceGroup);
                    pollForCreation(authenticatedContext, azureClient, resourceGroup, deploymentName, networkLinkDeploymentId,
                            enabledPrivateEndpointServices, networkId);
                } else {
                    LOGGER.debug("Network links ({}) are not requested yet in resource group {}", enabledPrivateEndpointServices, resourceGroup);

                    if (isCreated(networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK)) {
                        LOGGER.debug("Network links deployment ({}) is there in database but not deployed on Azure, resetting it..", networkLinkDeploymentId);
                        updateCloudResource(authenticatedContext, deploymentName, networkLinkDeploymentId, CommonStatus.REQUESTED, AZURE_VIRTUAL_NETWORK_LINK);
                    } else {
                        persistCloudResource(authenticatedContext, deploymentName, networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK);
                    }

                    createMissingNetworkLinks(azureClient, azureNetworkId, resourceGroup, tags, enabledPrivateEndpointServices);
                    updateCloudResource(authenticatedContext, deploymentName, networkLinkDeploymentId, CommonStatus.CREATED, AZURE_VIRTUAL_NETWORK_LINK);
                }
            } catch (CloudException | DataAccessException e) {
                LOGGER.warn("Deployment {} failed due to {}", deploymentName, e.getMessage());
                pollForCreation(authenticatedContext, azureClient, resourceGroup, deploymentName, networkLinkDeploymentId,
                        enabledPrivateEndpointServices, networkId);
            }
        }
    }

    private void pollForCreation(AuthenticatedContext authenticatedContext, AzureClient azureClient, String resourceGroup, String deploymentName,
            String dnsZoneDeploymentId, List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices, String networkId) {
        AzureDnsZoneCreationCheckerContext checkerContext = new AzureDnsZoneCreationCheckerContext(azureClient,
                resourceGroup,
                deploymentName,
                networkId,
                enabledPrivateEndpointServices);
        azureDnsZoneCreationPoller.startPolling(authenticatedContext, checkerContext);
        CommonStatus deploymentStatus = azureClient.getTemplateDeploymentCommonStatus(resourceGroup, deploymentName);
        ResourceType resouceType = StringUtils.isEmpty(networkId) ? AZURE_PRIVATE_DNS_ZONE : AZURE_VIRTUAL_NETWORK_LINK;
        updateCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId, deploymentStatus, resouceType);
    }

    private void createDnsZonesAndNetworkLinks(AzureClient azureClient, String azureNetworkId, String resourceGroup,
            Map<String, String> tags, List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices) {
        AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters(azureNetworkId,
                false,
                enabledPrivateEndpointServices,
                resourceGroup,
                tags);
        deployTemplate(azureClient, parameters);
    }

    private Network getAzureNetwork(AzureClient azureClient, String networkId, String networkResourceGroup) {
        Network azureNetwork = azureClient.getNetworkByResourceGroup(networkResourceGroup, networkId);
        if (Objects.isNull(azureNetwork)) {
            throw new CloudConnectorException(String.format("Azure network id lookup failed with network id %s in resource group %s", networkId,
                    networkResourceGroup));
        }
        return azureNetwork;
    }

    private void createMissingNetworkLinks(AzureClient azureClient, String azureNetworkId, String resourceGroup,
            Map<String, String> tags, List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices) {
        for (AzurePrivateDnsZoneServiceEnum service: enabledPrivateEndpointServices) {
            PagedList<VirtualNetworkLinkInner> networkLinks = azureClient.listNetworkLinksByPrivateDnsZoneName(resourceGroup, service.getDnsZoneName());
            boolean networkLinkCreated = azureClient.isNetworkLinkCreated(StringUtils.substringAfterLast(azureNetworkId, "/"), networkLinks);
            if (!networkLinkCreated) {
                LOGGER.debug("Network links for service {} not yet created, creating them now", service.getSubResource());
                AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters(azureNetworkId,
                        true,
                        enabledPrivateEndpointServices,
                        resourceGroup,
                        tags);
                deployTemplate(azureClient, parameters);
            }
        }
    }

    private List<AzurePrivateDnsZoneServiceEnum> getEnabledPrivateEndpointServices() {
        return privateEndpointServices.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getBySubResource)
                .collect(Collectors.toList());
    }

    private void deployTemplate(AzureClient azureClient, AzureDnsZoneDeploymentParameters parameters) {
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = parameters.getEnabledPrivateEndpointServices();
        String resourceGroup = parameters.getResourceGroupName();

        LOGGER.debug("Deploying Private DNS Zones and applying network link for services {}",
                enabledPrivateEndpointServices.stream().map(AzurePrivateDnsZoneServiceEnum::getSubResource).collect(Collectors.toList()));
        String suffix = getDeploymentSuffix(parameters);
        String deploymentName = generateDeploymentName(enabledPrivateEndpointServices, suffix);

        try {
            // TODO: this code seems to create trouble when parallel creating many environments in a fresh RG
//            if (azureClient.templateDeploymentExists(resourceGroup, deploymentName)) {
//                LOGGER.debug("Deleting already existing deployment {}", deploymentName);
//                azureClient.deleteTemplateDeployment(resourceGroup, deploymentName);
//            }
            String template = azureNetworkDnsZoneTemplateBuilder.build(parameters);
            String parametersMapAsString = new Json(Map.of()).getValue();

            LOGGER.debug("Creating deployment with name {} in resource group {}", deploymentName, resourceGroup);
            azureClient.createTemplateDeployment(resourceGroup, deploymentName, template, parametersMapAsString);
        } catch (CloudException e) {
            LOGGER.info("Provisioning error, cloud exception happened: ", e);
            throw azureUtils.convertToCloudConnectorException(e, "DNS Zone and network link template deployment");
        } catch (Exception e) {
            LOGGER.warn("Provisioning error:", e);
            throw new CloudConnectorException(String.format("Error in provisioning network dns zone template %s: %s",
                    deploymentName, e.getMessage()));
        }
    }

    private String getDeploymentSuffix(AzureDnsZoneDeploymentParameters parameters) {
        String networkId = StringUtils.substringAfterLast(parameters.getNetworkId(), "/");
        return parameters.getDeployOnlyNetworkLinks() ? "-" + networkId + NETWORK_LINKS : DNS_ZONES;
    }

    private String generateDeploymentName(List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices, String suffix) {
        String fullDeploymentName = enabledPrivateEndpointServices.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getSubResource)
                .collect(Collectors.joining("-", "", suffix))
                .toLowerCase();
        String deploymentName = StringUtils.left(fullDeploymentName, DEPLOYMENT_LENGTH_LIMIT);
        LOGGER.debug("Generated deployment name {}", deploymentName);
        return deploymentName;
    }

    private boolean isRequested(String dnsZoneDeploymentId, ResourceType resourceType) {
        return findDeploymentByStatus(dnsZoneDeploymentId, CommonStatus.REQUESTED, resourceType).isPresent();
    }

    private boolean isCreated(String dnsZoneDeploymentId, ResourceType resourceType) {
        return findDeploymentByStatus(dnsZoneDeploymentId, CommonStatus.CREATED, resourceType).isPresent();
    }

    private void persistCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, ResourceType resourceType) {
        LOGGER.debug("Persisting {} deployment with REQUESTED status: {} and name {}", resourceType, deploymentId, deploymentName);
        persistenceNotifier.notifyAllocation(buildCloudResource(deploymentName, deploymentId, CommonStatus.REQUESTED, resourceType), ac.getCloudContext());
    }

    private void updateCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, CommonStatus commonStatus,
            ResourceType resourceType) {
        LOGGER.debug("Updating {} deployment to {}: {}", resourceType, commonStatus, deploymentId);
        persistenceNotifier.notifyUpdate(buildCloudResource(deploymentName, deploymentId, commonStatus, resourceType), ac.getCloudContext());
    }

    private CloudResource buildCloudResource(String name, String dnsZoneDeploymentId, CommonStatus status, ResourceType resourceType) {
        return CloudResource.builder()
                .name(name)
                .status(status)
                .persistent(true)
                .reference(dnsZoneDeploymentId)
                .type(resourceType)
                .build();
    }

    private Optional<CloudResource> findDeploymentByStatus(String dnsZoneDeploymentId, CommonStatus status, ResourceType resourceType) {
        return resourcePersistenceRetriever.notifyRetrieve(dnsZoneDeploymentId, status, resourceType);
    }
}
