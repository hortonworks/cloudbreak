package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class SubnetSelectorTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> NETWORK_CIDRS = Set.of("1.2.3.4/32", "0.0.0.0/0");

    @InjectMocks
    private SubnetSelector underTest;

    @Test
    public void testNoSubnetInAvailabilityZone() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));

        Optional<CloudSubnet> subnet = underTest.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(), AZ_1, true);

        assert subnet.isEmpty();
    }

    @Test
    public void testSubnetFoundInAvailabilityZone() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));

        Optional<CloudSubnet> subnet = underTest.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(), AZ_1, true);

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetFromEndpointSubnetst() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)));

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isPresent();
        assertEquals(PUBLIC_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetFromEnvironmentSubnetst() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of(
            "key1", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1),
            "key2", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)
        ));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isPresent();
        assertEquals(PUBLIC_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEndpointSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isEmpty();
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEnvironmentSubnets() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isEmpty();
    }

    @Test
    public void testChooseEndpointGatewaySubnetFromEndpointSubnetstRoutableToInternet() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", getRoutableToInternetCloudSubnet(PRIVATE_ID_1, AZ_1)));

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetFromEnvironmentSubnetstRoutableToInternet() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of(
            "key1", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1),
            "key2", getRoutableToInternetCloudSubnet(PUBLIC_ID_1, AZ_1)
        ));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        Optional<CloudSubnet> subnet =
            underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1);

        assert subnet.isPresent();
        assertEquals(PUBLIC_ID_1, subnet.get().getId());
    }

    private CloudSubnet getCloudSubnet(String availabilityZone) {
        return new CloudSubnet(PRIVATE_ID_1, "name", availabilityZone, "cidr");
    }

    private EnvironmentNetworkResponse setupResponse() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setNetworkCidrs(NETWORK_CIDRS);
        source.setOutboundInternetTraffic(OutboundInternetTraffic.DISABLED);
        return source;
    }
}
