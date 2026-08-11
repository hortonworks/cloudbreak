package com.sequenceiq.cloudbreak.cloud.azure.template;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.resources.models.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureFallbackAwareDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStackViewProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateDeploymentRequest;
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
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.cloudbreak.service.retry.RetryService;

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

    @Inject
    private AzureFallbackAwareDeploymentService azureFallbackAwareDeploymentService;

    public Deployment getTemplateDeployment(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView,
            AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        String template = getRenderedTemplate(stack, azureStackView, ac, stackName, client, azureInstanceTemplateOperation).template();
        String parameters = azureTemplateBuilder.buildParameters();

        return retry.testWith1SecDelayMax5Times(() -> submit(() ->
                client.createTemplateDeployment(resourceGroupName, stackName, template, parameters)));
    }

    public Deployment getTemplateDeploymentWithFallback(AzureClient client, CloudStack stack, AuthenticatedContext ac, AzureStackView azureStackView,
            AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        RenderedTemplate rendered = getRenderedTemplate(stack, azureStackView, ac, stackName, client, azureInstanceTemplateOperation);
        String parameters = azureTemplateBuilder.buildParameters();
        AzureTemplateDeploymentRequest request = new AzureTemplateDeploymentRequest(
                client, resourceGroupName, stackName, rendered.template(), parameters, azureStackView,
                cloudContext, stack, rendered.credentialView(), rendered.customImageId(), azureInstanceTemplateOperation, rendered.marketplaceImage());

        return retry.testWith1SecDelayMax5Times(() -> submit(() ->
                azureFallbackAwareDeploymentService.createTemplateDeploymentWithFallback(request)));
    }

    private Deployment submit(Supplier<Deployment> action) {
        try {
            return action.get();
        } catch (ManagementException e) {
            if (e.getValue() != null && e.getValue().getDetails() != null) {
                String details = e.getValue().getDetails().stream().map(ManagementError::getMessage).collect(Collectors.joining(", "));
                if (details.contains("Please check the power state later")) {
                    throw new Retry.ActionFailedException("VMs not started in time.", e);
                }
            }
            throw e;
        }
    }

    public Optional<ManagementError> runWhatIfAnalysis(AzureClient client, CloudStack stack, AuthenticatedContext ac) {
        CloudContext cloudContext = ac.getCloudContext();
        String stackName = azureUtils.generateResourceNameByNameAndId("whatif", cloudContext.getId().toString());
        String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack);
        AzureStackView azureStackView = azureStackViewProvider
                .getAzureStack(new AzureCredentialView(ac.getCloudCredential()), stack, client, ac);
        String template = getRenderedTemplate(stack, azureStackView, ac, stackName, client, AzureInstanceTemplateOperation.PROVISION).template();
        return client.runWhatIfAnalysis(resourceGroupName, stackName, template);
    }

    private RenderedTemplate getRenderedTemplate(CloudStack stack, AzureStackView azureStackView, AuthenticatedContext ac,
            String stackName, AzureClient client, AzureInstanceTemplateOperation azureInstanceTemplateOperation) {
        Image stackImage = stack.getImage();
        CloudContext cloudContext = ac.getCloudContext();
        AzureCredentialView credentialView = createCredential(ac);
        if (azureImageFormatValidator.isMarketplaceImageFormat(stackImage)) {
            AzureMarketplaceImage marketplaceImage = azureMarketplaceImageProviderService.get(stackImage);
            String template = azureTemplateBuilder.build(stackName, null, credentialView, azureStackView, cloudContext, stack, azureInstanceTemplateOperation,
                    marketplaceImage);
            return new RenderedTemplate(template, null, marketplaceImage, credentialView);
        }
        String customImageId = azureStorage.getCustomImage(client, ac, stack).getId();
        AzureMarketplaceImage sourcePlanImage = azureImageFormatValidator.hasSourceImagePlan(stackImage)
                ? azureMarketplaceImageProviderService.getSourceImage(stackImage) : null;
        String template = azureTemplateBuilder.build(stackName, customImageId, credentialView, azureStackView, cloudContext, stack,
                azureInstanceTemplateOperation, sourcePlanImage);
        return new RenderedTemplate(template, customImageId, sourcePlanImage, credentialView);
    }

    private AzureCredentialView createCredential(AuthenticatedContext ac) {
        return new AzureCredentialView(ac.getCloudCredential());
    }

    private record RenderedTemplate(String template, String customImageId, AzureMarketplaceImage marketplaceImage, AzureCredentialView credentialView) {
    }

}