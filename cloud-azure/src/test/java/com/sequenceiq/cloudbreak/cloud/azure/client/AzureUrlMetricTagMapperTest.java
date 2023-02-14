package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import okhttp3.Request;

class AzureUrlMetricTagMapperTest {

    private AzureUrlMetricTagMapper underTest = new AzureUrlMetricTagMapper();

    // CHECKSTYLE:OFF
    @Test
    void testRequestUrlMapping() {
        assertEquals("microsoft.compute", underTest.apply(request("https://management.azure.com/subscriptions/randomId/providers/Microsoft.Compute/locations/westus2/vmSizes?api-version=2022-08-01")));
        assertEquals("microsoft.compute", underTest.apply(request("https://westus2.management.azure.com/subscriptions/randomId/resourceGroups/azure-sdx2191/providers/Microsoft.Compute/virtualMachines?api-version=2022-08-01")));
        assertEquals("microsoft.managedidentity", underTest.apply(request("https://management.azure.com/subscriptions/randomId/resourceGroups/rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/identity?api-version=2018-11-30")));
        assertEquals("login", underTest.apply(request("https://login.microsoftonline.com/randomId/oauth2/v2.0/token")));
        assertEquals("none", underTest.apply(request("https://management.azure.com/subscriptions/randomId?api-version=2021-01-01")));
        assertEquals("none", underTest.apply(request("https://management.azure.com/subscriptions/randomId?api-version=2021-01-01")));
        assertEquals("operationresults", underTest.apply(request("https://westus2.management.azure.com/subscriptions/randomId/operationresults/jobId?api-version=2021-01-01")));
        assertEquals("resourcegroups", underTest.apply(request("https://westus2.management.azure.com/subscriptions/randomId/resourcegroups/rgname?api-version=2021-01-01")));
    }
    // CHECKSTYLE:ON

    private static Request request(String url) {
        return new Request.Builder().url(url).build();
    }

}