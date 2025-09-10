package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentBaseNetworkConverterTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> NETWORK_CIDRS = Set.of("1.2.3.4/32", "0.0.0.0/0");

    private static final String EU_AZ = "eu-west-1a";

    @InjectMocks
    private TestEnvironmentBaseNetworkConverter underTest;

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private EntitlementService entitlementService;

    @Test
    void testConvertToLegacyNetworkWhenSubnetNotFound() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));
        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), any())).thenReturn(Optional.empty());

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, EU_AZ))
        );
        assertEquals(badRequestException.getMessage(), "No subnet for the given availability zone: eu-west-1a");
    }

    @Test
    void testConvertToLegacyNetworkWhenSubnetFound() {
        CloudSubnet subnet = getCloudSubnet(EU_AZ);
        Map<String, CloudSubnet> metas = Map.of("key", subnet);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(metas);

        when(subnetSelector.chooseSubnet(any(), eq(metas), eq(EU_AZ), eq(SelectionFallbackStrategy.ALLOW_FALLBACK)))
            .thenReturn(Optional.of(subnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, EU_AZ);
        });
        assertEquals("eu-west-1", network[0].getAttributes().getString("subnetId"));
        assertTrue(network[0].getNetworkCidrs().containsAll(NETWORK_CIDRS));
        assertEquals(source.getOutboundInternetTraffic(), network[0].getOutboundInternetTraffic());
    }

    @Test
    void testConvertToLegacyNetworkWithEndpointAccessGateway() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), eq(source.getSubnetMetas()), eq(AZ_1), eq(SelectionFallbackStrategy.ALLOW_FALLBACK)))
            .thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(eq(source), eq(privateSubnet.getId()))).thenReturn(Optional.of(publicSubnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertEquals(PUBLIC_ID_1, network[0].getAttributes().getString(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    void testConvertToLegacyNetworkWithPrivateEndpointAccessGateway() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PRIVATE_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(true);
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        source.setEndpointGatewaySubnetIds(Set.of(PRIVATE_ID_1));
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), eq(source.getSubnetMetas()), eq(AZ_1), eq(SelectionFallbackStrategy.ALLOW_FALLBACK)))
                .thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(eq(source), eq(privateSubnet.getId()))).thenReturn(Optional.of(publicSubnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertEquals(PRIVATE_ID_1, network[0].getAttributes().getString(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    void testConvertToLegacyNetworkWithEndpointAccessGatewayNoPublicSubnets() {
        CloudSubnet privateSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", privateSubnet));

        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), any())).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, AZ_1))
        );
        assertEquals("Could not find public subnet in availability zone: AZ-1", exception.getMessage());
    }

    @Test
    void testEndpointGatewayIsDisabled() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), any())).thenReturn(Optional.of(privateSubnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertNull(network[0].getAttributes().getString(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet.Builder()
                .id("eu-west-1")
                .name("name")
                .availabilityZone(availabilityZone)
                .cidr("cidr")
                .build();
    }

    private EnvironmentNetworkResponse setupResponse() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setNetworkCidrs(NETWORK_CIDRS);
        source.setOutboundInternetTraffic(OutboundInternetTraffic.DISABLED);
        return source;
    }

    private static class TestEnvironmentBaseNetworkConverter extends EnvironmentBaseNetworkConverter {

        @Override
        Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
            return Collections.emptyMap();
        }

        @Override
        public CloudPlatform getCloudPlatform() {
            return CloudPlatform.AWS;
        }
    }
}