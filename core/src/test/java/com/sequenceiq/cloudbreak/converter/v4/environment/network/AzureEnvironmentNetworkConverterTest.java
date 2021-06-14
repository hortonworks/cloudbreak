package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@MockitoSettings
class AzureEnvironmentNetworkConverterTest {

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private SubnetSelector subnetSelector;

    @InjectMocks
    private AzureEnvironmentNetworkConverter converter;

    @Test
    void testGetAttributesForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkAzureParams azure = mock(EnvironmentNetworkAzureParams.class);
        when(environmentNetworkResponse.getAzure()).thenReturn(azure);
        when(azure.getNetworkId()).thenReturn("my_network_id");
        when(azure.getResourceGroupName()).thenReturn("my_resource_group");
        when(azure.getNoPublicIp()).thenReturn(true);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertEquals("my_network_id", result.get("networkId"));
        assertEquals("my_resource_group", result.get("resourceGroupName"));
        assertEquals(true, result.get("noPublicIp"));
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, converter.getCloudPlatform());
    }
}