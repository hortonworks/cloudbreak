package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public record AzureTemplateDeploymentRequest(
        AzureClient client,
        String resourceGroupName,
        String stackName,
        String initialTemplate,
        String parameters,
        AzureStackView azureStackView,
        CloudContext cloudContext,
        CloudStack cloudStack,
        AzureCredentialView credentialView,
        String customImageId,
        AzureInstanceTemplateOperation operation,
        AzureMarketplaceImage azureMarketplaceImage,
        Set<String> skippableLoadBalancers) {

    public AzureTemplateDeploymentRequest {
        skippableLoadBalancers = skippableLoadBalancers == null ? Set.of() : Set.copyOf(skippableLoadBalancers);
    }
}
