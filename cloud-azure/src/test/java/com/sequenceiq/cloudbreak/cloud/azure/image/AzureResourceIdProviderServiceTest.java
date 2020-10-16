package com.sequenceiq.cloudbreak.cloud.azure.image;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;

public class AzureResourceIdProviderServiceTest {

    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final String IMAGE_NAME = "image-name";

    private static final String DEPLOYMENT_NAME = "deployment-name";

    private static final String RESOURCE_GROUP = "resource-group";

    private static final String NETWORK_ID = "network-id";

    private static final String SERVICE_ID = "privatelink.postgres.database.azure.com";

    private final AzureResourceIdProviderService underTest = new AzureResourceIdProviderService();

    @Test
    public void testGenerateImageIdShouldCreateValidImageId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Compute/images/image-name";

        String actual = underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, IMAGE_NAME);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGenerateDnsZoneDeploymentIdShouldCreateValidDnsZoneId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Resources/deployments/deployment-name";

        String actual = underTest.generateDeploymentId(SUBSCRIPTION_ID, RESOURCE_GROUP, DEPLOYMENT_NAME);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGenerateNetworkLinkIdShouldCreateValidNetworkLinkId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Network/privateDnsZones"
                + "/privatelink.postgres.database.azure.com/virtualNetworkLinks/network-id";

        String actual = underTest.generateNetworkLinkId(SUBSCRIPTION_ID, RESOURCE_GROUP, SERVICE_ID, NETWORK_ID);

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateImageIdShouldThrowExceptionWhenSubscriptionIdIsNull() {
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

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenSubscriptionIdIsNull() {
        underTest.generateImageId(null, RESOURCE_GROUP, IMAGE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenResourceGroupIsNull() {
        underTest.generateImageId(SUBSCRIPTION_ID, null, IMAGE_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenDeploymentIdIsNull() {
        underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, null);
    }
}