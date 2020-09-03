package com.sequenceiq.cloudbreak.cloud.azure.template;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class AzureTemplateDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureTemplateDeploymentService.class);

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    public Deployment getTemplateDeployment(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView,
            AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String template = getTemplate(stack, azureStackView, ac, ac.getCloudContext(), stackName, client, azureInstanceTemplateOperation);
        String parameters = azureTemplateBuilder.buildParameters();
        if (!client.templateDeploymentExists(resourceGroupName, stackName) || azureInstanceTemplateOperation == AzureInstanceTemplateOperation.UPSCALE) {
            Deployment templateDeployment = client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
            LOGGER.debug("Created template deployment for launch: {}", templateDeployment.exportTemplate().template());
            return templateDeployment;
        } else {
            Deployment templateDeployment = client.getTemplateDeployment(resourceGroupName, stackName);
            LOGGER.debug("Get template deployment for launch as it exists: {}", templateDeployment.exportTemplate().template());
            return templateDeployment;
        }
    }

    private String getTemplate(CloudStack stack, AzureStackView azureStackView, AuthenticatedContext ac, CloudContext cloudContext,
            String stackName, AzureClient client, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
//        String customImageId = azureStorage.getCustomImage(client, ac, stack).getId();
        return azureTemplateBuilder.build(stackName, createCredential(ac), azureStackView, cloudContext, stack, azureInstanceTemplateOperation);
    }

    private AzureCredentialView createCredential(AuthenticatedContext ac) {
        return new AzureCredentialView(ac.getCloudCredential());
    }

}
