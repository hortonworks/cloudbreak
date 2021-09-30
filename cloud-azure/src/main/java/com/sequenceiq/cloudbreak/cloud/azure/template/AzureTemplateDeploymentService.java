package com.sequenceiq.cloudbreak.cloud.azure.template;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryService;

@Component
public class AzureTemplateDeploymentService {

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private RetryService retry;

    public Deployment getTemplateDeployment(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView,
            AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String template = getTemplate(stack, azureStackView, ac, ac.getCloudContext(), stackName, client, azureInstanceTemplateOperation);
        String parameters = azureTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());

        return retry.testWith1SecDelayMax5Times(() -> {
            try {
                return client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
            } catch (CloudException e) {
                if (e.body() != null && e.body().details() != null) {
                    String details = e.body().details().stream().map(CloudError::message).collect(Collectors.joining(", "));
                    if (details.contains("Please check the power state later")) {
                        throw new Retry.ActionFailedException("VMs not started in time.", e);
                    }
                }
                throw e;
            }
        });
    }

    private String getTemplate(CloudStack stack, AzureStackView azureStackView, AuthenticatedContext ac, CloudContext cloudContext,
            String stackName, AzureClient client, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        String template;
        Image stackImage = stack.getImage();
        if (azureImageFormatValidator.isMarketplaceImageFormat(stackImage)) {
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(stack.getImage());
            template = azureTemplateBuilder.build(stackName, null, createCredential(ac), azureStackView, cloudContext, stack, azureInstanceTemplateOperation,
                    azureMarketplaceImage);
        } else {
            String customImageId = azureStorage.getCustomImage(client, ac, stack).getId();
            template = azureTemplateBuilder
                    .build(stackName, customImageId, createCredential(ac), azureStackView, cloudContext, stack, azureInstanceTemplateOperation, null);
        }
        return template;
    }

    private AzureCredentialView createCredential(AuthenticatedContext ac) {
        return new AzureCredentialView(ac.getCloudCredential());
    }

}
