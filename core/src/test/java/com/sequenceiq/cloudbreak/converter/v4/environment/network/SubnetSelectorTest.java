package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
public class SubnetSelectorTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final Set<String> NETWORK_CIDRS = Set.of("1.2.3.4/32", "0.0.0.0/0");

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SubnetSelector underTest;

    @Test
    public void testNoSubnetInAvailabilityZone() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet("any")));

        Optional<CloudSubnet> subnet = underTest.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(),
            AZ_1, SelectionFallbackStrategy.ALLOW_FALLBACK);

        assert subnet.isEmpty();
    }

    @Test
    public void testSubnetFoundInAvailabilityZone() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));

        Optional<CloudSubnet> subnet = underTest.chooseSubnet(source.getPreferedSubnetId(), source.getSubnetMetas(),
            AZ_1, SelectionFallbackStrategy.ALLOW_FALLBACK);

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetFromEndpointSubnetst() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getCloudSubnet(AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("public-key", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)));
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(false);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

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
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(false);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

        assert subnet.isPresent();
        assertEquals(PUBLIC_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEndpointSubnetsEndpointGatewayValidationEnabled() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(false);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

        assert subnet.isEmpty();
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEndpointSubnetsEndpointGatewayValidationDisabled() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        source.setGatewayEndpointSubnetMetas(Map.of("private-key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(true);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEnvironmentSubnetsEndpointGatewayValidationEnabled() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(false);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

        assert subnet.isEmpty();
    }

    @Test
    public void testChooseEndpointGatewaySubnetNoPublicEnvironmentSubnetsEndpointGatewayValidationDisabled() {
        EnvironmentNetworkResponse source = setupResponse();
        source.setSubnetMetas(Map.of("key", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));
        source.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        when(entitlementService.endpointGatewaySkipValidation(any())).thenReturn(true);

        Optional<CloudSubnet> subnet =
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.chooseSubnetForEndpointGateway(source, PRIVATE_ID_1));

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
    }

    @Test
    public void testChooseSubnetPreferPublic() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of(
            "key1", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1),
            "key2", getPublicCloudSubnet(PUBLIC_ID_1, AZ_1)
        ));

        // Loop to ensure we select the public subnet every time
        for (int i = 0; i < 10; i++) {
            Optional<CloudSubnet> subnet = underTest.chooseSubnetPreferPublic(null, source.getSubnetMetas(), AZ_1);
            assert subnet.isPresent();
            assertEquals(PUBLIC_ID_1, subnet.get().getId());
        }
    }

    @Test
    public void testChooseSubnetPreferPublicWhenNoPublicSubnet() {
        EnvironmentNetworkResponse source = new EnvironmentNetworkResponse();
        source.setSubnetMetas(Map.of("key1", getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1)));

        Optional<CloudSubnet> subnet = underTest.chooseSubnetPreferPublic(null, source.getSubnetMetas(), AZ_1);

        assert subnet.isPresent();
        assertEquals(PRIVATE_ID_1, subnet.get().getId());
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
