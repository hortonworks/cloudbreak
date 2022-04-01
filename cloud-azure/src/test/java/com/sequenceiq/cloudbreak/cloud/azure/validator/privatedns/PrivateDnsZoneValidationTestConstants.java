package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

public class PrivateDnsZoneValidationTestConstants {

    static final String SUBSCRIPTION_ID = "subscriptionId";

    static final String SINGLE_RESOURCE_GROUP_NAME = "single-resource-group-name";

    static final String A_RESOURCE_GROUP_NAME = "a-resource-group-name";

    static final String ZONE_NAME_POSTGRES = "privatelink.postgres.database.azure.com";

    static final String PRIVATE_DNS_ZONE_ID = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/privateDnsZones/%s";

    static final String NETWORK_SUBSCRIPTION_ID = "networkSubscriptionId";

    static final String NETWORK_RESOURCE_GROUP_NAME = "networkResourceGroupName";

    static final String NETWORK_NAME = "networkName";

    static final String NETWORK_RESOURCE_ID = "/subscriptions/" + NETWORK_SUBSCRIPTION_ID + "/resourceGroups/" + NETWORK_RESOURCE_GROUP_NAME +
            "/providers/Microsoft.Network/virtualNetworks/" + NETWORK_NAME;

    private PrivateDnsZoneValidationTestConstants() {
    }

    static String getPrivateDnsZoneId() {
        return getPrivateDnsZoneId(SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES);
    }

    static String getPrivateDnsZoneId(String resourceGroupName) {
        return String.format(PRIVATE_DNS_ZONE_ID, SUBSCRIPTION_ID, resourceGroupName, ZONE_NAME_POSTGRES);
    }

    static String getPrivateDnsZoneId(String resourceGroupName, String zoneName) {
        return String.format(PRIVATE_DNS_ZONE_ID, SUBSCRIPTION_ID, resourceGroupName, zoneName);
    }

}
