package com.sequenceiq.environment.network.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class EnvironmentBaseNetworkConverterTest {

    private static final long NETWORK_ID = 123L;

    private static final String NETWORK_NAME = "networkName";

    private static final String NETWORK_CIDR = "cidr";

    private static final String NETWORK_CRN = "networkCrn";

    private static final String VPC_ID = "vpcId";

    private static final Map<String, CloudSubnet> SUBNET_1 =
            Map.of("subnet1", new CloudSubnet("123", "subnet1", "az", "cidr1", true, false, false, SubnetType.PRIVATE));

    private static final Map<String, CloudSubnet> SUBNET_2 =
            Map.of("subnet2", new CloudSubnet("234", "subnet2", "az", "cidr2", true, false, false, SubnetType.PRIVATE));

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private TestEnvironmentBaseNetworkConverter underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] scenarios() {
        return new Object[][] {
                { "Null endpointgw subnets, targeting disabled",  SUBNET_1, null,     true,  subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.ALL), Map.of() },
                { "Null endpointgw subnets, targeting enabled",   SUBNET_1, null,     false, subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS), Map.of() },
                { "Empty endpointgw subnets, targeting disabled", SUBNET_1, Map.of(), true,  subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.ALL), Map.of() },
                { "Empty endpointgw subnets, targeting enabled",  SUBNET_1, Map.of(), false, subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS), Map.of() },
                { "Endpointgw subnets, targeting disabled",       SUBNET_1, SUBNET_2, true,  subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.ALL), subnetResult("234", "subnet2", "cidr2", DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS) },
                { "Endpointgw subnets, targeting enabled",        SUBNET_1, SUBNET_2, false, subnetResult("123", "subnet1", "cidr1", DeploymentRestriction.NON_ENDPOINT_ACCESS_GATEWAYS), subnetResult("234", "subnet2", "cidr2", DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS) }

        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private static  Map<String, CloudSubnet> subnetResult(String id, String name, String cidr, Set<DeploymentRestriction> restrictions) {
        CloudSubnet subnet = new CloudSubnet(id, name, "az", cidr, true, false, false, SubnetType.PRIVATE);
        subnet.setDeploymentRestrictions(restrictions);
        return Map.of(name, subnet);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void testConvertToDto(String testName, Map<String, CloudSubnet> subnetMetas, Map<String, CloudSubnet> endpointGwSubnetMetas, boolean targetingEnabled,
            Map<String, CloudSubnet> expectedSubnetMetas, Map<String, CloudSubnet> expectedEndpointGwSubnetMetas) {
        AwsNetwork network = getAwsNetwork();
        network.setEndpointGatewaySubnetMetas(endpointGwSubnetMetas);
        network.setSubnetMetas(subnetMetas);
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(targetingEnabled);

        NetworkDto result = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> underTest.convertToDto(network));

        assertConversion(result, expectedSubnetMetas, expectedEndpointGwSubnetMetas);
    }

    private void assertConversion(NetworkDto result, Map<String, CloudSubnet> expectedSubnetMetas, Map<String, CloudSubnet> expectedEndpointGwSubnetMetas) {
        assertThat(result.getId()).isEqualTo(NETWORK_ID);
        assertThat(result.getLoadBalancerCreation()).isEqualTo(LoadBalancerCreation.ENABLED);
        assertThat(result.getName()).isEqualTo(NETWORK_NAME);
        assertThat(result.getNetworkCidr()).isEqualTo(NETWORK_CIDR);
        assertThat(result.getNetworkCidrs()).isEqualTo(Set.of(NETWORK_CIDR));
        assertThat(result.getNetworkId()).isEqualTo(VPC_ID);
        assertThat(result.getOutboundInternetTraffic()).isEqualTo(OutboundInternetTraffic.ENABLED);
        assertThat(result.getPrivateSubnetCreation()).isEqualTo(PrivateSubnetCreation.ENABLED);
        assertThat(result.getPublicEndpointAccessGateway()).isEqualTo(PublicEndpointAccessGateway.ENABLED);
        assertThat(result.getResourceCrn()).isEqualTo(NETWORK_CRN);
        assertThat(result.getRegistrationType()).isEqualTo(RegistrationType.EXISTING);
        assertThat(result.getServiceEndpointCreation()).isEqualTo(ServiceEndpointCreation.ENABLED);
        assertThat(result.getSubnetMetas()).isEqualTo(expectedSubnetMetas);
        assertThat(result.getEndpointGatewaySubnetMetas()).isEqualTo(expectedEndpointGwSubnetMetas);
    }

    private AwsNetwork getAwsNetwork() {
        AwsNetwork network = new AwsNetwork();
        network.setId(NETWORK_ID);
        network.setLoadBalancerCreation(LoadBalancerCreation.ENABLED);
        network.setName(NETWORK_NAME);
        network.setNetworkCidr(NETWORK_CIDR);
        network.setOutboundInternetTraffic(OutboundInternetTraffic.ENABLED);
        network.setPrivateSubnetCreation(PrivateSubnetCreation.ENABLED);
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        network.setRegistrationType(RegistrationType.EXISTING);
        network.setResourceCrn(NETWORK_CRN);
        network.setServiceEndpointCreation(ServiceEndpointCreation.ENABLED);
        network.setVpcId(VPC_ID);
        return network;
    }

    private static class TestEnvironmentBaseNetworkConverter extends EnvironmentBaseNetworkConverter {

        protected TestEnvironmentBaseNetworkConverter(EnvironmentViewConverter environmentViewConverter, EntitlementService entitlementService) {
            super(environmentViewConverter, entitlementService);
        }

        @Override
        public BaseNetwork setCreatedCloudNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
            return null;
        }

        @Override
        public CloudPlatform getCloudPlatform() {
            return CloudPlatform.AWS;
        }

        @Override
        public boolean isApplicableForDwx(CloudSubnet cloudSubnet) {
            return false;
        }

        @Override
        public boolean isApplicableForMlx(CloudSubnet cloudSubnet) {
            return false;
        }

        @Override
        BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
            return new AwsNetwork();
        }

        @Override
        NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork source) {
            return builder.build();
        }

        @Override
        void setRegistrationType(BaseNetwork result, NetworkDto networkDto) {
            result.setRegistrationType(RegistrationType.EXISTING);
        }
    }
}
