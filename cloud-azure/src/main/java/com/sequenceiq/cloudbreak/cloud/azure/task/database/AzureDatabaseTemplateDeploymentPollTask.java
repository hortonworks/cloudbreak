package com.sequenceiq.cloudbreak.cloud.azure.task.database;

import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.DELETED;
import static com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(AzureDatabaseTemplateDeploymentPollTask.NAME)
@Scope("prototype")
public class AzureDatabaseTemplateDeploymentPollTask extends PollBooleanStateTask {

    public static final String NAME = "AzureTemplateDeploymentPollTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseTemplateDeploymentPollTask.class);

    private final AzureDatabaseTemplateDeploymentContext deploymentContext;

    public AzureDatabaseTemplateDeploymentPollTask(AuthenticatedContext authenticatedContext,
            AzureDatabaseTemplateDeploymentContext azureDatabaseTemplateDeploymentContext) {
        super(authenticatedContext, false);
        deploymentContext = azureDatabaseTemplateDeploymentContext;
    }

    @Override
    protected Boolean doCall() {
        AzureClient azureClient = deploymentContext.getAzureClient();
        String resourceGroupName = deploymentContext.getAzureTemplateDeploymentParameters().getResourceGroupName();
        String deploymentName = deploymentContext.getAzureTemplateDeploymentParameters().getTemplateName();

        ResourceStatus templateDeploymentStatus = azureClient.getTemplateDeploymentStatus(resourceGroupName, deploymentName);
        if (templateDeploymentStatus.isPermanent()) {
            LOGGER.info("Deployment {} has been finished with status {}", deploymentName, templateDeploymentStatus);
            checkDeploymentStatus(resourceGroupName, deploymentName, templateDeploymentStatus);
            return true;
        } else {
            LOGGER.debug("Azure template deployment has not finished yet: {}.", deploymentName);
            return false;
        }
    }

    private void checkDeploymentStatus(String resourceGroupName, String deploymentName, ResourceStatus templateDeploymentStatus) {
        if (DELETED == templateDeploymentStatus) {
            throwException("Deployment %s in resource group %s is either deleted or does not exist", deploymentName, resourceGroupName);
        }

        if (FAILED == templateDeploymentStatus) {
            throwException("Deployment %s in resource group %s was either cancelled or failed.", deploymentName, resourceGroupName);
        }
    }

    private void throwException(String format, String deploymentName, String resourceGroupName) {
        String message = String.format(format, deploymentName, resourceGroupName);
        LOGGER.warn(message);
        throw new CloudConnectorException(message);
    }

}