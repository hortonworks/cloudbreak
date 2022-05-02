package com.sequenceiq.cloudbreak.cloud.azure.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.database.PollingStarter;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@Service
public class AzureTemplateCreatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateCreatorService.class);

    public Deployment createOrPollTemplateDeployment(AzureClient azureClient,
            AzureTemplateDeploymentParameters templateDeploymentParameters, PollingStarter deploymentPoller) throws Exception {
        String resourceGroupName = templateDeploymentParameters.getResourceGroupName();
        String deploymentName = templateDeploymentParameters.getTemplateName();
        if (shouldCreateTemplateDeployment(azureClient, resourceGroupName, deploymentName)) {
            LOGGER.debug("Creating deployment {} in RG {}", deploymentName, resourceGroupName);
            return azureClient.createTemplateDeployment(templateDeploymentParameters);
        } else {
            LOGGER.debug("Deployment {} in RG {} is in progress, starting polling", deploymentName, resourceGroupName);
            deploymentPoller.startPolling();
            return azureClient.getTemplateDeployment(resourceGroupName, deploymentName);
        }
    }

    private boolean shouldCreateTemplateDeployment(AzureClient client, String resourceGroupName, String stackName) {
        boolean deploymentExists = client.templateDeploymentExists(resourceGroupName, stackName);
        LOGGER.debug("Checking the existence of deployment {} in RG {}: {}", stackName, resourceGroupName, deploymentExists);
        return !deploymentExists || client.getTemplateDeploymentStatus(resourceGroupName, stackName) != ResourceStatus.IN_PROGRESS;
    }

}
