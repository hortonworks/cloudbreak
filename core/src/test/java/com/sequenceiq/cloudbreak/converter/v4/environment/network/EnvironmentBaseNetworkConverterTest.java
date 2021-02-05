package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBaseNetworkConverterTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> NETWORK_CIDRS = Set.of("1.2.3.4/32", "0.0.0.0/0");

    private static final String EU_AZ = "eu-west-1a";

    @InjectMocks
    private TestEnvironmentBaseNetworkConverter underTest;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SubnetSelector subnetSelector;

    @Test
    public void testConvertToLegacyNetworkWhenSubnetNotFound() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));
        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), anyBoolean())).thenReturn(Optional.empty());

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, EU_AZ))
        );
        assertEquals(badRequestException.getMessage(), "No subnet for the given availability zone: eu-west-1a");
    }

    @Test
    public void testConvertToLegacyNetworkWhenSubnetFound() {
        CloudSubnet subnet = getCloudSubnet(EU_AZ);
        Map<String, CloudSubnet> metas = Map.of("key", subnet);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(metas);

        when(subnetSelector.chooseSubnet(any(), eq(metas), eq(EU_AZ), eq(true))).thenReturn(Optional.of(subnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, EU_AZ);
        });
        assertEquals(network[0].getAttributes().getValue("subnetId"), "eu-west-1");
        assertTrue(network[0].getNetworkCidrs().containsAll(NETWORK_CIDRS));
        assertEquals(source.getOutboundInternetTraffic(), network[0].getOutboundInternetTraffic());
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGateway() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), eq(source.getSubnetMetas()), eq(AZ_1), eq(true))).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(eq(source), eq(privateSubnet.getId()))).thenReturn(Optional.of(publicSubnet));
        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertEquals(PUBLIC_ID_1, network[0].getAttributes().getValue(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGatewayNoPublicSubnets() {
        CloudSubnet privateSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", privateSubnet));

        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), anyBoolean())).thenReturn(Optional.of(privateSubnet));
        when(subnetSelector.chooseSubnetForEndpointGateway(any(), anyString())).thenReturn(Optional.empty());
        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Exception exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, AZ_1))
        );
        assertEquals("Could not find public subnet in availability zone: AZ-1", exception.getMessage());
    }

    @Test
    public void testEndpointGatewaySubnetNotSetWhenEntitlementIsDisabled() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), anyBoolean())).thenReturn(Optional.of(privateSubnet));
        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(false);

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertNull(network[0].getAttributes().getValue(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    @Test
    public void testEndpointGatewayIsDisabledd() {
        CloudSubnet privateSubnet = getCloudSubnet(AZ_1);
        CloudSubnet publicSubnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", privateSubnet));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", publicSubnet));

        when(subnetSelector.chooseSubnet(any(), anyMap(), anyString(), anyBoolean())).thenReturn(Optional.of(privateSubnet));

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertNull(network[0].getAttributes().getValue(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
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
