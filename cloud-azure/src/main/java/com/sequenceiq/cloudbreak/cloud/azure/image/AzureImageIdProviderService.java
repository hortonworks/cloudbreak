package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;

@Component
public class AzureImageIdProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageIdProviderService.class);

    public String generateImageId(String subscriptionId, String resourceGroup, String imageName) {
        Assert.hasText(subscriptionId, "Subscription id must not be null or empty.");
        Assert.hasText(resourceGroup, "Resource group must not be null or empty.");
        Assert.hasText(imageName, "Image name must not be null or empty.");

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/subscriptions/");
        stringBuilder.append(subscriptionId);
        stringBuilder.append("/resourceGroups/");
        stringBuilder.append(resourceGroup);
        stringBuilder.append("/providers/Microsoft.Compute/");
        stringBuilder.append(AzureStorage.IMAGES_CONTAINER);
        stringBuilder.append("/");
        stringBuilder.append(imageName);
        String resourceReference = stringBuilder.toString();
        LOGGER.info("Generated resourceReferenceId: {}", resourceReference);
        return resourceReference;
    }
}
