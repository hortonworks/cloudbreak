package com.sequenceiq.cloudbreak.cloud.azure.task.dnszone;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AzureDnsZoneCreationCheckerTask.NAME)
@Scope("prototype")
public class AzureDnsZoneCreationCheckerTask extends PollBooleanStateTask {

    public static final String NAME = "AzureDnsZoneCreationChecker";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDnsZoneCreationCheckerTask.class);

    private final AzureDnsZoneCreationCheckerContext context;

    public AzureDnsZoneCreationCheckerTask(AuthenticatedContext authenticatedContext, AzureDnsZoneCreationCheckerContext context) {
        super(authenticatedContext, false);
        this.context = context;
    }

    @Override
    protected Boolean doCall() {
        String deploymentName = context.getDeploymentName();
        String resourceGroupName = context.getResourceGroupName();
        AzureClient azureClient = context.getAzureClient();
        String networkId = context.getNetworkId();
        List<AzurePrivateDnsZoneServiceEnum> enabledPrivateEndpointServices = context.getEnabledPrivateEndpointServices();
        LOGGER.info("Waiting for DNS zone deployment to be created: {}", deploymentName);

        ResourceStatus templateDeploymentStatus = azureClient.getTemplateDeploymentStatus(resourceGroupName, deploymentName);

        if (templateDeploymentStatus == ResourceStatus.DELETED) {
            throw new CloudConnectorException(String.format("Deployment %s is either deleted or does not exist", deploymentName));
        }

        if (templateDeploymentStatus.isPermanent()) {
            LOGGER.info("Deployment has been finished with status {}", templateDeploymentStatus);

            if (StringUtils.isNotEmpty(networkId)) {
                return azureClient.checkIfNetworkLinksDeployed(resourceGroupName, networkId, enabledPrivateEndpointServices);
            } else {
                return azureClient.checkIfDnsZonesDeployed(resourceGroupName, enabledPrivateEndpointServices);
            }
        } else {
            LOGGER.info("DNS zone or network link creation not finished yet.");
            return false;
        }
    }
}
