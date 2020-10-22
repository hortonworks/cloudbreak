package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.common.api.type.CommonStatus;

@Service
public class AzureDnsZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDnsZoneService.class);

    private static final String DNS_ZONES = "-dns-zones";

    @Inject
    private AzureResourceDeploymentHelperService azureResourceDeploymentHelperService;

    @Inject
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Inject
    private AzureResourcePersistenceHelperService azureResourcePersistenceHelperService;

    @Inject
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    public void checkOrCreateDnsZones(AuthenticatedContext authenticatedContext, AzureClient azureClient, AzureNetworkView networkView,
            String resourceGroup, Map<String, String> tags) {

        String networkId = networkView.getNetworkId();
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = azurePrivateEndpointServicesProvider.getEnabledPrivateEndpointServices();
        boolean dnsZonesDeployed = azureClient.checkIfDnsZonesDeployed(resourceGroup, enabledPrivateEndpointServices);

        if (!dnsZonesDeployed) {
            LOGGER.debug("Dns zones are not deployed yet!");
            String networkResourceGroup = networkView.getResourceGroupName();
            String deploymentName = azureResourceDeploymentHelperService.generateDeploymentName(enabledPrivateEndpointServices, DNS_ZONES);
            String dnsZoneDeploymentId = azureResourceIdProviderService.generateDeploymentId(azureClient.getCurrentSubscription().subscriptionId(),
                    resourceGroup, deploymentName);
            String azureNetworkId = azureResourceDeploymentHelperService.getAzureNetwork(azureClient, networkId, networkResourceGroup).id();
            AzureDnsZoneCreationCheckerContext checkerContext = new AzureDnsZoneCreationCheckerContext(
                    azureClient, resourceGroup, deploymentName, dnsZoneDeploymentId, null, enabledPrivateEndpointServices);
            try {
                if (azureResourcePersistenceHelperService.isRequested(dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE)) {
                    LOGGER.debug("Dns zones ({}) already requested in resource group {}", enabledPrivateEndpointServices, resourceGroup);
                    azureResourceDeploymentHelperService.pollForCreation(authenticatedContext, checkerContext);
                } else {
                    LOGGER.debug("Dns zones ({}) are not requested yet in resource group {}, creating them..", enabledPrivateEndpointServices, resourceGroup);

                    persistResource(authenticatedContext, deploymentName, dnsZoneDeploymentId);
                    createDnsZonesAndNetworkLinks(azureClient, azureNetworkId, resourceGroup, tags, enabledPrivateEndpointServices);
                    azureResourcePersistenceHelperService.updateCloudResource(
                            authenticatedContext, deploymentName, dnsZoneDeploymentId, CommonStatus.CREATED, AZURE_PRIVATE_DNS_ZONE);

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
        }
    }

    public void persistResource(AuthenticatedContext authenticatedContext, String deploymentName, String dnsZoneDeploymentId) {
        if (azureResourcePersistenceHelperService.isCreated(dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE)) {
            LOGGER.debug("Dns zone deployment ({}) is there in database but not deployed on Azure, resetting it..", dnsZoneDeploymentId);
            azureResourcePersistenceHelperService.updateCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId,
                    CommonStatus.REQUESTED, AZURE_PRIVATE_DNS_ZONE);
        } else {
            azureResourcePersistenceHelperService.persistCloudResource(authenticatedContext, deploymentName, dnsZoneDeploymentId,
                    AZURE_PRIVATE_DNS_ZONE);
        }
    }

    private void createDnsZonesAndNetworkLinks(AzureClient azureClient, String azureNetworkId, String resourceGroup,
            Map<String, String> tags, List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices) {
        AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters(azureNetworkId,
                false,
                enabledPrivateEndpointServices,
                resourceGroup,
                tags);
        azureResourceDeploymentHelperService.deployTemplate(azureClient, parameters);
    }
}