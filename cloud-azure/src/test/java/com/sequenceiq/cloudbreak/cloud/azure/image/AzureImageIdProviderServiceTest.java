package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.junit.Assert;
import org.junit.Test;

public class AzureImageIdProviderServiceTest {

    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final String IMAGE_NAME = "image-name";

    private static final String RESOURCE_GROUP = "resource-group";

    private AzureImageIdProviderService underTest = new AzureImageIdProviderService();

    @Test
    public void testGenerateImageIdShouldCreateImageId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Compute/images/image-name";

        String actual = underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, IMAGE_NAME);

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateImageIdShouldThrowExceptionWhenTheSubscriptionIdIsNull() {
        underTest.generateImageId(null, RESOURCE_GROUP, IMAGE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateImageIdShouldThrowExceptionWhenResourceGroupIsNull() {
        underTest.generateImageId(SUBSCRIPTION_ID, null, IMAGE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateImageIdShouldThrowExceptionWhenTheImageNameIsNull() {
        underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, null);
    }
}