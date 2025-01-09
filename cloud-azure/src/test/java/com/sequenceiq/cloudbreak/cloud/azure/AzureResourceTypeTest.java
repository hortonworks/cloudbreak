package com.sequenceiq.cloudbreak.cloud.azure;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sequenceiq.common.api.type.ResourceType;

class AzureResourceTypeTest {

    @ParameterizedTest
    @CsvSource({
            "AZURE_DATABASE, DATABASE_SERVER",
            "AZURE_DATABASE_CANARY, DATABASE_SERVER",
            "AZURE_PRIVATE_ENDPOINT, PRIVATE_ENDPOINT",
            "AZURE_PRIVATE_ENDPOINT_CANARY, PRIVATE_ENDPOINT",
            "AZURE_DNS_ZONE_GROUP, PRIVATE_DNS_ZONE_GROUP",
            "AZURE_DNS_ZONE_GROUP_CANARY, PRIVATE_DNS_ZONE_GROUP",
            "AZURE_RESOURCE_GROUP, RESOURCE_GROUP"
    })
    public void testGetByResourceTypeWithParameterizedTest(ResourceType resourceType, AzureResourceType expected) {
        AzureResourceType result = AzureResourceType.getByResourceType(resourceType);
        assertEquals(expected, result);
        assertEquals(expected.getAzureType(), result.getAzureType());
    }
}