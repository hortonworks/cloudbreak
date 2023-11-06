package com.sequenceiq.cloudbreak.cloud.azure.template;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStackViewProvider;
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
    private AzureStackViewProvider azureStackViewProvider;

    @Inject
    private RetryService retry;

    public Deployment getTemplateDeployment(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView,
            AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String template = getTemplate(stack, azureStackView, ac, stackName, client, azureInstanceTemplateOperation);
        String parameters = azureTemplateBuilder.buildParameters(ac.getCloudCredential(), stack.getNetwork(), stack.getImage());

        return retry.testWith1SecDelayMax5Times(() -> {
            try {
                return client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
            } catch (ManagementException e) {
                if (e.getValue() != null && e.getValue().getDetails() != null) {
                    String details = e.getValue().getDetails().stream().map(ManagementError::getMessage).collect(Collectors.joining(", "));
                    if (details.contains("Please check the power state later")) {
                        throw new Retry.ActionFailedException("VMs not started in time.", e);
                    }
                }
                throw e;
            }
        });
    }

    public Optional<ManagementError> runWhatIfAnalysis(AzureClient client, CloudStack stack, AuthenticatedContext ac) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.generateResourceGroupNameByNameAndId("whatif", cloudContext.getId().toString());
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        AzureStackView azureStackView = azureStackViewProvider
                .getAzureStack(new AzureCredentialView(ac.getCloudCredential()), stack, client, ac);
        String template = getTemplate(stack, azureStackView, ac, stackName, client, AzureInstanceTemplateOperation.PROVISION);
        return client.runWhatIfAnalysis(resourceGroupName, stackName, template);
    }

    private String getTemplate(CloudStack stack, AzureStackView azureStackView, AuthenticatedContext ac,
            String stackName, AzureClient client, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        String template;
        Image stackImage = stack.getImage();
        CloudContext cloudContext = ac.getCloudContext();
        if (azureImageFormatValidator.isMarketplaceImageFormat(stackImage)) {
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(stackImage);
            template = azureTemplateBuilder.build(stackName, null, createCredential(ac), azureStackView, cloudContext, stack, azureInstanceTemplateOperation,
                    azureMarketplaceImage);
        } else {
            String customImageId = azureStorage.getCustomImage(client, ac, stack).getId();
            template = azureTemplateBuilder
                    .build(stackName, customImageId, createCredential(ac), azureStackView, cloudContext, stack, azureInstanceTemplateOperation,
                            azureImageFormatValidator.hasSourceImagePlan(stackImage) ? azureMarketplaceImageProviderService.getSourceImage(stackImage) : null);
        }
        return template;
    }

    private AzureCredentialView createCredential(AuthenticatedContext ac) {
        return new AzureCredentialView(ac.getCloudCredential());
    }

}
