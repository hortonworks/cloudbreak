package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class GcpEnvironmentNetworkConverterTest {

    public static final String GCP_NETWORK_ID = "my_gcp_network";

    public static final String GCP_SHARED_PROJECT_ID = "my_shared_project";

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private SubnetSelector subnetSelector;

    @InjectMocks
    private GcpEnvironmentNetworkConverter converter;

    @Test
    void selectPublicSubnetForEndpointGateway() {
    }

    @Test
    void getNetworkIdForAttributeLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkGcpParams environmentNetworkGcpParams = mock(EnvironmentNetworkGcpParams .class);
        when(environmentNetworkResponse.getGcp()).thenReturn(environmentNetworkGcpParams);
        when(environmentNetworkGcpParams.getNetworkId()).thenReturn(GCP_NETWORK_ID);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertEquals(GCP_NETWORK_ID, result.get("networkId"));
    }

    @Test
    void getNoFirewallRulesAttributeForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkGcpParams environmentNetworkGcpParams = mock(EnvironmentNetworkGcpParams .class);
        when(environmentNetworkResponse.getGcp()).thenReturn(environmentNetworkGcpParams);
        when(environmentNetworkGcpParams.getNoFirewallRules()).thenReturn(true);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertEquals(true, result.get("noFirewallRules"));
    }

    @Test
    void getNoPublicIpAttributeForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkGcpParams environmentNetworkGcpParams = mock(EnvironmentNetworkGcpParams .class);
        when(environmentNetworkResponse.getGcp()).thenReturn(environmentNetworkGcpParams);
        when(environmentNetworkGcpParams.getNoPublicIp()).thenReturn(true);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertEquals(true, result.get("noPublicIp"));
    }

    @Test
    void getSharedProjectIdAttributeForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkGcpParams environmentNetworkGcpParams = mock(EnvironmentNetworkGcpParams .class);
        when(environmentNetworkResponse.getGcp()).thenReturn(environmentNetworkGcpParams);
        when(environmentNetworkGcpParams.getSharedProjectId()).thenReturn(GCP_SHARED_PROJECT_ID);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertEquals(GCP_SHARED_PROJECT_ID, result.get("sharedProjectId"));
    }

    @Test
    void testAllNullAttributesForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkGcpParams environmentNetworkGcpParams = mock(EnvironmentNetworkGcpParams.class);
        when(environmentNetworkResponse.getGcp()).thenReturn(environmentNetworkGcpParams);
        when(environmentNetworkGcpParams.getNetworkId()).thenReturn(null);
        when(environmentNetworkGcpParams.getNoFirewallRules()).thenReturn(null);
        when(environmentNetworkGcpParams.getNoPublicIp()).thenReturn(null);
        when(environmentNetworkGcpParams.getSharedProjectId()).thenReturn(null);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertFalse(result.containsKey("networkId"));
    }

    @Test
    void getCloudPlatform() {
        assertEquals(CloudPlatform.GCP, converter.getCloudPlatform());
    }
}