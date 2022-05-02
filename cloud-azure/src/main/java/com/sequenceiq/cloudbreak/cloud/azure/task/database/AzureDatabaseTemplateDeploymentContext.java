package com.sequenceiq.cloudbreak.cloud.azure.task.database;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentParameters;

public class AzureDatabaseTemplateDeploymentContext {

    private final AzureClient azureClient;

    private final AzureTemplateDeploymentParameters azureTemplateDeploymentParameters;

    public AzureDatabaseTemplateDeploymentContext(AzureClient azureClient, AzureTemplateDeploymentParameters azureTemplateDeploymentParameters) {
        this.azureClient = azureClient;
        this.azureTemplateDeploymentParameters = azureTemplateDeploymentParameters;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public AzureTemplateDeploymentParameters getAzureTemplateDeploymentParameters() {
        return azureTemplateDeploymentParameters;
    }

}
