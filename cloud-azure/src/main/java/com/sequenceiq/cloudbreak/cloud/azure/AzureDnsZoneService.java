package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
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

    @Inject
    private AzureCloudResourceService azureCloudResourceService;

    public List<CloudResource> checkOrCreateDnsZones(AuthenticatedContext authenticatedContext, AzureClient azureClient, AzureNetworkView networkView,
            String resourceGroup, Map<String, String> tags, Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone,
            PrivateDatabaseVariant privateEndpointVariant) {

        String networkId = networkView.getNetworkId();
        List<AzureManagedPrivateDnsZoneService> cdpManagedDnsZones = azurePrivateEndpointServicesProvider
                .getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone, privateEndpointVariant);
        boolean dnsZonesDeployed = azureClient.checkIfDnsZonesDeployed(resourceGroup, cdpManagedDnsZones);
        String deploymentName = azureResourceDeploymentHelperService.generateDeploymentName(cdpManagedDnsZones, DNS_ZONES);

        if (!dnsZonesDeployed) {
            LOGGER.debug("Dns zones are not deployed yet!");
            String subscriptionId = azureClient.getCurrentSubscription().subscriptionId();
            String networkResourceGroup = networkView.getResourceGroupName();
            String dnsZoneDeploymentId = azureResourceIdProviderService.generateDeploymentId(subscriptionId,
                    resourceGroup, deploymentName);
            String azureNetworkId = azureResourceDeploymentHelperService.getAzureNetwork(azureClient, networkId, networkResourceGroup).id();
            AzureDnsZoneCreationCheckerContext checkerContext = new AzureDnsZoneCreationCheckerContext(
                    azureClient, resourceGroup, deploymentName, dnsZoneDeploymentId, null, cdpManagedDnsZones);
            try {
                if (azureResourcePersistenceHelperService.isRequested(dnsZoneDeploymentId, AZURE_PRIVATE_DNS_ZONE)) {
                    LOGGER.debug("Dns zones ({}) already requested in resource group {}", cdpManagedDnsZones, resourceGroup);
                    return azureCloudResourceService.getDeploymentCloudResources(azureResourceDeploymentHelperService.pollForCreation(
                            authenticatedContext, checkerContext));
                } else {
                    LOGGER.debug("Dns zones ({}) are not requested yet in resource group {}, creating them..", cdpManagedDnsZones, resourceGroup);

                    persistResource(authenticatedContext, deploymentName, dnsZoneDeploymentId);
                    Deployment deployment = createDnsZonesAndNetworkLinks(azureClient, azureNetworkId, resourceGroup, tags, cdpManagedDnsZones);
                    azureResourcePersistenceHelperService.updateCloudResource(
                            authenticatedContext, deploymentName, dnsZoneDeploymentId, CommonStatus.CREATED, AZURE_PRIVATE_DNS_ZONE);
                    return azureCloudResourceService.getDeploymentCloudResources(deployment);
                }
            } catch (CloudConnectorException e) {
                LOGGER.warn("Deployment {} failed due to {}", deploymentName, e.getMessage());
                azureResourceDeploymentHelperService.pollForCreation(authenticatedContext, checkerContext);
                throw e;

                // DataAccessException is thrown if multiple parallel launches
                // would cause edge case of inserting multiple db record violating the unique constraint
            } catch (DataAccessException e) {
                LOGGER.warn("Polling {} deployment due to db unique constraint violation: {}", deploymentName, e.getMessage());
                return azureCloudResourceService.getDeploymentCloudResources(azureResourceDeploymentHelperService.pollForCreation(
                        authenticatedContext, checkerContext));
            }
        } else {
            Deployment templateDeployment = azureClient.getTemplateDeployment(resourceGroup, deploymentName);
            return azureCloudResourceService.getDeploymentCloudResources(templateDeployment);
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

    private Deployment createDnsZonesAndNetworkLinks(AzureClient azureClient, String azureNetworkId, String resourceGroup,
            Map<String, String> tags, List<AzureManagedPrivateDnsZoneService> enabledPrivateEndpointServices) {
        AzureDnsZoneDeploymentParameters parameters = new AzureDnsZoneDeploymentParameters(azureNetworkId,
                false,
                enabledPrivateEndpointServices,
                resourceGroup,
                tags);
        return azureResourceDeploymentHelperService.deployTemplate(azureClient, parameters);
    }
}
