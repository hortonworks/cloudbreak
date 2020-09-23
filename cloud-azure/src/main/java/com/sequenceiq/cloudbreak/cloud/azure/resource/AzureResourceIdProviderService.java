package com.sequenceiq.cloudbreak.cloud.azure.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;

@Component
public class AzureResourceIdProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceIdProviderService.class);

    public String generateImageId(String subscriptionId, String resourceGroup, String imageName) {
        Assert.hasText(subscriptionId, "Subscription id must not be null or empty.");
        Assert.hasText(resourceGroup, "Resource group must not be null or empty.");
        Assert.hasText(imageName, "Image name must not be null or empty.");

        String resourceReference = String.format("%s/providers/Microsoft.Compute/%s/%s",
                generateCommonPart(subscriptionId, resourceGroup),
                AzureStorage.IMAGES_CONTAINER,
                imageName);
        LOGGER.info("Generated resourceReferenceId: {}", resourceReference);
        return resourceReference;
    }

    public String generateDeploymentId(String subscriptionId, String resourceGroup, String deploymentName) {
        Assert.hasText(subscriptionId, "Subscription id must not be null or empty.");
        Assert.hasText(resourceGroup, "Resource group must not be null or empty.");
        Assert.hasText(deploymentName, "Deployment name must not be null or empty.");

        String resourceReference = String.format("%s/providers/Microsoft.Resources/deployments/%s",
                generateCommonPart(subscriptionId, resourceGroup),
                deploymentName);
        LOGGER.info("Generated resourceReferenceId: {}", resourceReference);
        return resourceReference;
    }

    public String generateNetworkLinkId(String subscriptionId, String resourceGroup, String service, String networkid) {
        Assert.hasText(subscriptionId, "Subscription id must not be null or empty.");
        Assert.hasText(resourceGroup, "Resource group must not be null or empty.");
        Assert.hasText(service, "Service name must not be null or empty.");
        Assert.hasText(networkid, "Network id must not be null or empty.");

        String resourceReference = String.format("%s/providers/Microsoft.Network/privateDnsZones/%s/virtualNetworkLinks/%s",
                generateCommonPart(subscriptionId, resourceGroup),
                service,
                networkid);
        LOGGER.info("Generated resourceReferenceId: {}", resourceReference);
        return resourceReference;
    }

    private String generateCommonPart(String subscriptionId, String resourceGroup) {
        return String.format("/subscriptions/%s/resourceGroups/%s", subscriptionId, resourceGroup);
    }
}
