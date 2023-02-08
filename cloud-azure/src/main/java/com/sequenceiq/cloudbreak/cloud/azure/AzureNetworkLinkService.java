package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_VIRTUAL_NETWORK_LINK;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.privatedns.fluent.models.VirtualNetworkLinkInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.common.api.type.CommonStatus;

@Service
public class AzureNetworkLinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureNetworkLinkService.class);

    private static final String NETWORK_LINKS = "-links";

    @Inject
    private AzureResourceDeploymentHelperService azureResourceDeploymentHelperService;

    @Inject
    private AzureResourcePersistenceHelperService azureResourcePersistenceHelperService;

    @Inject
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    @Inject
    private AzureResourceIdProviderService azureResourceIdProviderService;

    public void checkOrCreateNetworkLinks(AuthenticatedContext authenticatedContext, AzureClient azureClient, AzureNetworkView networkView,
            String resourceGroup, Map<String, String> tags,
            Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone) {

        String networkId = networkView.getNetworkId();
        String networkResourceGroup = networkView.getResourceGroupName();
        List<AzurePrivateDnsZoneServiceEnum> cdpManagedDnsZones = azurePrivateEndpointServicesProvider
                .getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone);

        boolean networkLinksDeployed = azureClient.checkIfNetworkLinksDeployed(resourceGroup, networkId, cdpManagedDnsZones);

        if (!networkLinksDeployed) {
            LOGGER.debug("Deploying network links that are not deployed yet!");
            String deploymentName = azureResourceDeploymentHelperService.
                    generateDeploymentName(cdpManagedDnsZones, "-" + networkId + NETWORK_LINKS);
            String networkLinkDeploymentId = azureResourceIdProviderService.generateDeploymentId(azureClient.getCurrentSubscription().subscriptionId(),
                    resourceGroup, deploymentName);
            AzureDnsZoneCreationCheckerContext checkerContext = new AzureDnsZoneCreationCheckerContext(
                    azureClient, resourceGroup, deploymentName, networkLinkDeploymentId, networkId, cdpManagedDnsZones);

            try {
                if (azureResourcePersistenceHelperService.isRequested(networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK)) {
                    LOGGER.debug("Network links ({}) already requested in resource group {}", cdpManagedDnsZones, resourceGroup);

                    azureResourceDeploymentHelperService.pollForCreation(authenticatedContext, checkerContext);
                } else {
                    LOGGER.debug("Network links ({}) are not requested yet in resource group {}", cdpManagedDnsZones, resourceGroup);

                    persistResource(authenticatedContext, deploymentName, networkLinkDeploymentId);
                    String azureNetworkId = azureResourceDeploymentHelperService.getAzureNetwork(azureClient, networkId, networkResourceGroup).id();
                    createMissingNetworkLinks(azureClient, azureNetworkId, resourceGroup, tags, cdpManagedDnsZones);
                    azureResourcePersistenceHelperService.updateCloudResource(
                            authenticatedContext, deploymentName, networkLinkDeploymentId, CommonStatus.CREATED, AZURE_VIRTUAL_NETWORK_LINK);
                }
            } catch (CloudConnectorException e) {
                LOGGER.warn("Deployment {} failed due to {}", deploymentName, e.getMessage());
                azureResourceDeploymentHelperService.pollForCreation(authenticatedContext, checkerContext);
                throw e;

                // DataAccessException is thrown if multiple parallel launches
                // would cause edge case of inserting multiple db record violating the unique constraint
            } catch (DataAccessException e) {
                LOGGER.warn("Polling {} deployment due to db unique constraint violation: {}", deploymentName, e.getMessage());
                azureResourceDeploymentHelperService.pollForCreation(authenticatedContext, checkerContext);
            }
        } else {
            LOGGER.debug("Dns zones ({}) and network links already deployed in resource group {}", cdpManagedDnsZones, resourceGroup);
        }
    }

    private void persistResource(AuthenticatedContext authenticatedContext, String deploymentName, String networkLinkDeploymentId) {
        if (azureResourcePersistenceHelperService.isCreated(networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK)) {
            LOGGER.debug("Network links deployment ({}) is there in database but not deployed on Azure, resetting it..", networkLinkDeploymentId);
            azureResourcePersistenceHelperService.updateCloudResource(
                    authenticatedContext, deploymentName, networkLinkDeploymentId, CommonStatus.REQUESTED, AZURE_VIRTUAL_NETWORK_LINK);
        } else {
            azureResourcePersistenceHelperService.persistCloudResource(
                    authenticatedContext, deploymentName, networkLinkDeploymentId, AZURE_VIRTUAL_NETWORK_LINK);
        }
    }

    private void createMissingNetworkLinks(AzureClient azureClient, String azureNetworkId, String resourceGroup,
            Map<String, String> tags, List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices) {
        for (AzurePrivateDnsZoneServiceEnum service : enabledPrivateEndpointServices) {
            AzureListResult<VirtualNetworkLinkInner> networkLinks = azureClient.listNetworkLinksByPrivateDnsZoneName(resourceGroup, service.getDnsZoneName());
            boolean networkLinkCreated = azureClient.isNetworkLinkCreated(StringUtils.substringAfterLast(azureNetworkId, "/"), networkLinks);
            if (!networkLinkCreated) {
                LOGGER.debug("Network links for service {} not yet created, creating them now", service.getSubResource());
                AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters(azureNetworkId,
                        true,
                        enabledPrivateEndpointServices,
                        resourceGroup,
                        tags);
                azureResourceDeploymentHelperService.deployTemplate(azureClient, parameters);
            }
        }
    }
}
