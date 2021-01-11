package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class EnvironmentBaseNetworkConverterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> NETWORK_CIDRS = Set.of("1.2.3.4/32", "0.0.0.0/0");

    private static final String AZ_1 = "AZ-1";

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String ENDPOINT_ID = "endpointGatewaySubnetId";

    @InjectMocks
    private TestEnvironmentBaseNetworkConverter underTest;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private EntitlementService entitlementService;

    @Test
    public void testConvertToLegacyNetworkWhenSubnetNotFound() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, "eu-west-1a"))
        );
        assertEquals(badRequestException.getMessage(), "No subnet for the given availability zone: eu-west-1a");
    }

    @Test
    public void testConvertToLegacyNetworkWhenSubnetFound() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("eu-west-1a")));
        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, "eu-west-1a");
        });
        assertEquals(network[0].getAttributes().getValue("subnetId"), "eu-west-1");
        assertTrue(network[0].getNetworkCidrs().containsAll(NETWORK_CIDRS));
        assertEquals(source.getOutboundInternetTraffic(), network[0].getOutboundInternetTraffic());
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGatewayAndProvidedSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)));

        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertEquals(PUBLIC_ID_1, network[0].getAttributes().getValue(ENDPOINT_ID));
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGatewayAndEnvironmentSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of(
            "key1", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1),
            "key2", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)
        ));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertEquals(PUBLIC_ID_1, network[0].getAttributes().getValue(ENDPOINT_ID));
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGatewayProvidedSubnetsNoPublicSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));

        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Exception exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, AZ_1))
        );
        assertEquals("Could not find public subnet in availability zone: AZ-1", exception.getMessage());
    }

    @Test
    public void testConvertToLegacyNetworkWithEndpointAccessGatewayEnvironmentSubnetsNoPublicSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);

        Exception exception = assertThrows(BadRequestException.class, () ->
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convertToLegacyNetwork(source, AZ_1))
        );
        assertEquals("Could not find public subnet in availability zone: AZ-1", exception.getMessage());
    }

    @Test
    public void testEndpointGatewaySubnetNotSetWhenEntitlementIsDisabled() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)));

        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(false);

        Network[] network = new Network[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            network[0] = underTest.convertToLegacyNetwork(source, AZ_1);
        });

        assertNull(network[0].getAttributes().getValue(ENDPOINT_ID));
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet("eu-west-1", "name", availabilityZone, "cidr");
    }

    private CloudSubnet getPublicCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet(id, "name", availabilityZone, "cidr", false, true, true, SubnetType.PUBLIC);
    }

    private CloudSubnet getPrivateCloudSubnet(String id, String availabilityZone) {
        return new CloudSubnet(id, "name", availabilityZone, "cidr", true, false, false, SubnetType.PRIVATE);
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
