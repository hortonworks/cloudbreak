package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.CLOUD_PLATFORM;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@MockitoSettings
class AzureEnvironmentNetworkConverterTest {

    private static final String DATABASE_PRIVATE_DNS_ZONE_ID = "my_database_private_dns_zone_id";

    private static final String DATABASE_PRIVATE_DNS_ZONE_ID_KEY = "databasePrivateDsZoneId";

    private static final String NETWORK_ID = "my_network_id";

    private static final String NETWORK_ID_KEY = "networkId";

    private static final String RESOURCE_GROUP_NAME = "my_resource_group";

    private static final String RESOURCE_GROUP_NAME_KEY = "resourceGroupName";

    private static final String NO_PUBLIC_IP_KEY = "noPublicIp";

    private static final String NO_OUTBOUND_LOAD_BALANCER_KEY = "noOutboundLoadBalancer";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AzureEnvironmentNetworkConverter converter;

    @Test
    void testConvertToLegacyNetworkWithPublicEndpointAccessGatewayEnabled() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkAzureParams azure = mock(EnvironmentNetworkAzureParams.class);
        CloudSubnet cloudSubnet = mock(CloudSubnet.class);
        when(environmentNetworkResponse.getPublicEndpointAccessGateway()).thenReturn(PublicEndpointAccessGateway.ENABLED);
        when(subnetSelector.chooseSubnet(any(), any(), any(), any())).thenReturn(Optional.of(cloudSubnet));

        when(cloudSubnet.getId()).thenReturn("my_cloud_subnet");

        when(environmentNetworkResponse.getAzure()).thenReturn(azure);
        when(azure.getNetworkId()).thenReturn(NETWORK_ID);
        when(azure.getResourceGroupName()).thenReturn(RESOURCE_GROUP_NAME);
        when(azure.getNoPublicIp()).thenReturn(true);

        Network result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> converter.convertToLegacyNetwork(environmentNetworkResponse, "my-az"));

        assertEquals(CloudPlatform.AZURE.toString(), result.getAttributes().getMap().get(CLOUD_PLATFORM).toString());
        assertFalse(result.getAttributes().getMap().containsKey(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    void testGetAttributesForLegacyNetwork() {
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        EnvironmentNetworkAzureParams azure = mock(EnvironmentNetworkAzureParams.class);
        when(environmentNetworkResponse.getAzure()).thenReturn(azure);
        when(azure.getNetworkId()).thenReturn(NETWORK_ID);
        when(azure.getResourceGroupName()).thenReturn(RESOURCE_GROUP_NAME);
        when(azure.getNoPublicIp()).thenReturn(true);
        when(azure.getDatabasePrivateDnsZoneId()).thenReturn(DATABASE_PRIVATE_DNS_ZONE_ID);
        when(azure.getNoOutboundLoadBalancer()).thenReturn(true);

        Map<String, Object> result = converter.getAttributesForLegacyNetwork(environmentNetworkResponse);

        assertThat(result).hasSize(5);
        assertEquals(NETWORK_ID, result.get(NETWORK_ID_KEY));
        assertEquals(RESOURCE_GROUP_NAME, result.get(RESOURCE_GROUP_NAME_KEY));
        assertEquals(true, result.get(NO_PUBLIC_IP_KEY));
        assertEquals(DATABASE_PRIVATE_DNS_ZONE_ID, result.get(DATABASE_PRIVATE_DNS_ZONE_ID_KEY));
        assertTrue((Boolean) result.get(NO_OUTBOUND_LOAD_BALANCER_KEY));
        assertFalse(result.containsKey(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, converter.getCloudPlatform());
    }

    @Test
    void testAttachEndpointGatewaySubnetIfTargeting() {
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        EnvironmentNetworkResponse environmentNetworkResponse = mock(EnvironmentNetworkResponse.class);
        when(environmentNetworkResponse.getPublicEndpointAccessGateway()).thenReturn(PublicEndpointAccessGateway.DISABLED);
        when(environmentNetworkResponse.getEndpointGatewaySubnetIds()).thenReturn(Set.of("endpointGwSubnet"));
        EnvironmentNetworkAzureParams azure = mock(EnvironmentNetworkAzureParams.class);
        when(azure.getNetworkId()).thenReturn(NETWORK_ID);
        when(azure.getResourceGroupName()).thenReturn(RESOURCE_GROUP_NAME);
        when(azure.getNoPublicIp()).thenReturn(true);
        when(azure.getDatabasePrivateDnsZoneId()).thenReturn(DATABASE_PRIVATE_DNS_ZONE_ID);
        when(azure.getNoOutboundLoadBalancer()).thenReturn(true);
        when(environmentNetworkResponse.getAzure()).thenReturn(azure);
        CloudSubnet cloudSubnet = mock(CloudSubnet.class);
        when(subnetSelector.chooseSubnet(any(), any(), any(), any())).thenReturn(Optional.of(cloudSubnet));

        Network result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> converter.convertToLegacyNetwork(environmentNetworkResponse, "my-az"));
        assertThat(result.getAttributes().getMap().containsKey(ENDPOINT_GATEWAY_SUBNET_ID)).isTrue();
        assertThat(result.getAttributes().getMap().get(ENDPOINT_GATEWAY_SUBNET_ID)).isEqualTo("endpointGwSubnet");
    }
}