package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_PRIVATE_DNS_ZONE;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VIRTUAL_NETWORK_LINK;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.network.Network;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureDnsZoneDeploymentParameters;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.dnszone.AzureDnsZoneCreationPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureResourceDeploymentHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceDeploymentHelperService.class);

    private static final int DEPLOYMENT_LENGTH_LIMIT = 64;

    private static final String DNS_ZONES = "-dns-zones";

    private static final String NETWORK_LINKS = "-links";

    @Inject
    private AzureNetworkDnsZoneTemplateBuilder azureNetworkDnsZoneTemplateBuilder;

    @Inject
    private AzureResourcePersistenceHelperService azureResourcePersistenceHelperService;

    @Inject
    private AzureDnsZoneCreationPoller azureDnsZoneCreationPoller;

    @Inject
    private AzureUtils azureUtils;

    public void pollForCreation(AuthenticatedContext authenticatedContext, AzureDnsZoneCreationCheckerContext checkerContext) {
        try {
            azureDnsZoneCreationPoller.startPolling(authenticatedContext, checkerContext);
        } catch (CloudConnectorException e) {
            LOGGER.warn("Exception during polling: {}", e.getMessage());
        } finally {
            AzureClient azureClient = checkerContext.getAzureClient();
            CommonStatus deploymentStatus = azureClient.getTemplateDeploymentCommonStatus(
                    checkerContext.getResourceGroupName(), checkerContext.getDeploymentName());
            ResourceType resourceType = StringUtils.isEmpty(checkerContext.getNetworkId()) ? AZURE_PRIVATE_DNS_ZONE : AZURE_VIRTUAL_NETWORK_LINK;
            azureResourcePersistenceHelperService.updateCloudResource(
                    authenticatedContext, checkerContext.getDeploymentName(), checkerContext.getDeploymentId(), deploymentStatus, resourceType);
        }
    }

    public Network getAzureNetwork(AzureClient azureClient, String networkId, String networkResourceGroup) {
        Network azureNetwork = azureClient.getNetworkByResourceGroup(networkResourceGroup, networkId);
        if (Objects.isNull(azureNetwork)) {
            throw new CloudConnectorException(String.format("Azure network id lookup failed with network id %s in resource group %s", networkId,
                    networkResourceGroup));
        }
        return azureNetwork;
    }

    public void deployTemplate(AzureClient azureClient, AzureDnsZoneDeploymentParameters parameters) {
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = parameters.getEnabledPrivateEndpointServices();
        String resourceGroup = parameters.getResourceGroupName();

        LOGGER.debug("Deploying Private DNS Zones and applying network link for services {}",
                enabledPrivateEndpointServices.stream().map(AzurePrivateDnsZoneServiceEnum::getSubResource).collect(Collectors.toList()));
        String suffix = getDeploymentSuffix(parameters);
        String deploymentName = generateDeploymentName(enabledPrivateEndpointServices, suffix);

        try {
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

    public String generateDeploymentName(List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices, String suffix) {
        String fullDeploymentName = enabledPrivateEndpointServices.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getSubResource)
                .collect(Collectors.joining("-", "", suffix))
                .toLowerCase();
        String deploymentName = StringUtils.left(fullDeploymentName, DEPLOYMENT_LENGTH_LIMIT);
        LOGGER.debug("Generated deployment name {}", deploymentName);
        return deploymentName;
    }

    private String getDeploymentSuffix(AzureDnsZoneDeploymentParameters parameters) {
        String networkId = StringUtils.substringAfterLast(parameters.getNetworkId(), "/");
        return parameters.getDeployOnlyNetworkLinks() ? "-" + networkId + NETWORK_LINKS : DNS_ZONES;
    }
}
