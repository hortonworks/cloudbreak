package com.sequenceiq.cloudbreak.cloud.azure.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateDnsZoneDeploymentIdShouldCreateValidDnsZoneId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Resources/deployments/deployment-name";

        String actual = underTest.generateDeploymentId(SUBSCRIPTION_ID, RESOURCE_GROUP, DEPLOYMENT_NAME);

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateNetworkLinkIdShouldCreateValidNetworkLinkId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Network/privateDnsZones"
                + "/privatelink.postgres.database.azure.com/virtualNetworkLinks/network-id";

        String actual = underTest.generateNetworkLinkId(SUBSCRIPTION_ID, RESOURCE_GROUP, SERVICE_ID, NETWORK_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateDnsZoneIdShouldCreateValidDnsZoneId() {
        String expected = "/subscriptions/subscription-id/resourceGroups/resource-group/providers/Microsoft.Network/privateDnsZones/"
                + "privatelink.postgres.database.azure.com";

        String actual = underTest.generateDnsZoneId(SUBSCRIPTION_ID, RESOURCE_GROUP, SERVICE_ID);

        assertEquals(expected, actual);
    }

    @Test
    public void testGenerateImageIdShouldThrowExceptionWhenSubscriptionIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(null, RESOURCE_GROUP, IMAGE_NAME);
        });

        assertEquals("Subscription id must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testGenerateImageIdShouldThrowExceptionWhenResourceGroupIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(SUBSCRIPTION_ID, null, IMAGE_NAME);
        });

        assertEquals("Resource group must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testGenerateImageIdShouldThrowExceptionWhenTheImageNameIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, null);
        });

        assertEquals("Image name must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenSubscriptionIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(null, RESOURCE_GROUP, IMAGE_NAME);
        });

        assertEquals("Subscription id must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenResourceGroupIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(SUBSCRIPTION_ID, null, IMAGE_NAME);
        });

        assertEquals("Resource group must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testGenerateDnsZoneDeploymentIdShouldThrowExceptionWhenDeploymentIdIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            underTest.generateImageId(SUBSCRIPTION_ID, RESOURCE_GROUP, null);
        });

        assertEquals("Image name must not be null or empty.", exception.getMessage());
    }
}