package com.sequenceiq.cloudbreak.cloud.azure.template;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AzureTemplateDeploymentService {

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    public Deployment getTemplateDeployment(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, stack);
        String template = getTemplate(stack, azureStackView, ac, ac.getCloudContext(), stackName, client);
        String parameters = azureTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());
        return client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
    }

    private String getTemplate(CloudStack stack, AzureStackView azureStackView, AuthenticatedContext ac, CloudContext cloudContext,
            String stackName, AzureClient client) {
        String customImageId = azureStorage.getCustomImageId(client, ac, stack);
        return azureTemplateBuilder.build(stackName, customImageId, createCredential(ac), azureStackView, cloudContext, stack);
    }

    private AzureCredentialView createCredential(AuthenticatedContext ac) {
        return new AzureCredentialView(ac.getCloudCredential());
    }

}
