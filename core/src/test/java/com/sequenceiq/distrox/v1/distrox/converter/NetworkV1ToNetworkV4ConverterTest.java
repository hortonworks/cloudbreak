package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.loadbalancer.EndpointGatewayNetworkValidator;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.AzureNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@ExtendWith(MockitoExtension.class)
class NetworkV1ToNetworkV4ConverterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String VPC_ID = "vpc-123";

    private static final String GROUP_NAME = "group-123";

    private static final String SUBNET_ID = "subnet-123";

    private static final String SUBNET2_ID = "subnet-456";

    private static final Set<String> SUBNET_IDS = Sets.newHashSet(SUBNET_ID, SUBNET2_ID);

    private static final String PUBLIC_SUBNET_ID = "public-subnet-123";

    private static final boolean NO_OUTBOUND_LOAD_BALANCER = true;

    @InjectMocks
    private NetworkV1ToNetworkV4Converter underTest;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private EndpointGatewayNetworkValidator endpointGatewayNetworkValidator;

    @Mock
    private EntitlementService entitlementService;

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithSubnet() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();


        NetworkV4Request[] networkV4Request = new NetworkV4Request[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            networkV4Request[0] = underTest
                    .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
        });

        assertEquals(networkV4Request[0].createAws().getVpcId(), VPC_ID);
        assertEquals(networkV4Request[0].createAws().getSubnetId(), SUBNET_ID);
    }

    @Test
    void testWhenEnvironmentResponseHasNoCloudPlatformSetThenIllegalStateExceptionShouldCome() {
        DetailedEnvironmentResponse input = createGcpEnvironment();
        input.setCloudPlatform(null);

        IllegalStateException expectedException = assertThrows(IllegalStateException.class, () ->
                underTest.convertToNetworkV4Request(new ImmutablePair<>(null, input)));

        assertEquals("Unable to determine cloud platform for network since it has not been set!", expectedException.getMessage());
    }

    @Test
    void testConvertToNetworkV4RequestWhenGcpNetworkKeyIsNullThenBasicSettingShouldHappen() {
        DetailedEnvironmentResponse input = createGcpEnvironment();
        NetworkV4Request result = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(null, input));

        assertNotNull(result);
        GcpNetworkV4Parameters gcpNetworkResult = result.getGcp();
        EnvironmentNetworkGcpParams inputGcpNetwork = input.getNetwork().getGcp();
        assertNotNull(gcpNetworkResult);
        assertEquals(inputGcpNetwork.getNetworkId(), gcpNetworkResult.getNetworkId());
        assertEquals(inputGcpNetwork.getNoPublicIp(), gcpNetworkResult.getNoPublicIp());
        assertEquals(inputGcpNetwork.getSharedProjectId(), gcpNetworkResult.getSharedProjectId());
        assertEquals(inputGcpNetwork.getNoFirewallRules(), gcpNetworkResult.getNoFirewallRules());
    }

    @Test
    void testConvertToNetworkV4RequestWhenMockNetworkKeyIsNullThenBasicSettingShouldHappen() {
        DetailedEnvironmentResponse input = createMockEnvironment();
        NetworkV4Request result = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(null, input));

        assertNotNull(result);
        MockNetworkV4Parameters mockNetworkResult = result.getMock();
        assertNotNull(mockNetworkResult);
        assertNull(mockNetworkResult.getVpcId());
        assertNull(mockNetworkResult.getInternetGatewayId());
        assertTrue(StringUtils.isNotEmpty(mockNetworkResult.getSubnetId()));
    }

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithoutSubnet() {
        NetworkV1Request networkV1Request = awsEmptyNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();

        NetworkV4Request[] networkV4Request = new NetworkV4Request[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            networkV4Request[0] = underTest
                    .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
        });

        assertEquals(networkV4Request[0].createAws().getVpcId(), VPC_ID);
        assertTrue(SUBNET_IDS.contains(networkV4Request[0].createAws().getSubnetId()));
    }

    @Test
    void testConvertToNetworkV1RequestWhenAwsNetworkParamIsNullThenItShouldNotBeSet() {
        NetworkV4Request input = createNetworkV4Request();
        input.setAws(null);

        NetworkV1Request result = underTest.convertToNetworkV1Request(input);

        assertNotNull(result);
        assertNull(result.getAws());
    }

    @Test
    void testConvertToNetworkV1RequestWhenAwsNetworkParamIsNotNullThenItShouldBeSet() {
        NetworkV4Request input = createNetworkV4Request();

        NetworkV1Request result = underTest.convertToNetworkV1Request(input);

        assertNotNull(result);
        assertNotNull(result.getAws());
        assertEquals(input.getAws().getSubnetId(), result.getAws().getSubnetId());
    }

    @Test
    void testConvertToNetworkV1RequestWhenAzureNetworkParamIsNullThenItShouldNotBeSet() {
        NetworkV4Request input = createNetworkV4Request();
        input.setAzure(null);

        NetworkV1Request result = underTest.convertToNetworkV1Request(input);

        assertNotNull(result);
        assertNull(result.getAzure());
    }

    @Test
    void testConvertToNetworkV1RequestWhenAzureNetworkParamIsNotNullThenItShouldBeSet() {
        NetworkV4Request input = createNetworkV4Request();

        NetworkV1Request result = underTest.convertToNetworkV1Request(input);

        assertNotNull(result);
        assertNotNull(result.getAzure());
        assertEquals(input.getAzure().getSubnetId(), result.getAzure().getSubnetId());
    }

    @Test
    void testConvertToStackRequestWhenAzurePresentedWithSubnet() {
        NetworkV1Request networkV1Request = azureNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = azureEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        assertEquals(networkV4Request.createAzure().getNetworkId(), VPC_ID);
        assertEquals(networkV4Request.createAzure().getResourceGroupName(), GROUP_NAME);
        assertEquals(networkV4Request.createAzure().getSubnetId(), SUBNET_ID);
        assertEquals(networkV4Request.createAzure().isNoOutboundLoadBalancer(), NO_OUTBOUND_LOAD_BALANCER);
    }

    @Test
    void testConvertToStackRequestWhenAzurePresentedWithoutSubnet() {
        NetworkV1Request networkV1Request = azureEmptyNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = azureEnvironmentNetwork();


        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        assertEquals(networkV4Request.createAzure().getNetworkId(), VPC_ID);
        assertEquals(networkV4Request.createAzure().getResourceGroupName(), GROUP_NAME);
        assertTrue(SUBNET_IDS.contains(networkV4Request.createAzure().getSubnetId()));
        assertEquals(networkV4Request.createAzure().isNoOutboundLoadBalancer(), NO_OUTBOUND_LOAD_BALANCER);
    }

    @Test
    void testConvertToStackRequestWhenYarnPresented() {
        NetworkV1Request networkV1Request = yarnNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = yarnEnvironmentNetwork();

        NetworkV4Request networkV4Request = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));

        assertEquals(1, networkV4Request.createYarn().asMap().size());
    }

    private NetworkV4Request createNetworkV4Request() {
        NetworkV4Request r = new NetworkV4Request();
        r.setAws(createAwsNetworkV4Parameters());
        r.setAzure(createAzureNetworkV4Parameters());
        return r;
    }

    private AzureNetworkV4Parameters createAzureNetworkV4Parameters() {
        AzureNetworkV4Parameters p = new AzureNetworkV4Parameters();
        p.setNetworkId("someNetworkId");
        p.setNoPublicIp(true);
        p.setSubnetId(SUBNET_ID);
        p.setResourceGroupName("someResourceGroupName");
        return p;
    }

    private AwsNetworkV4Parameters createAwsNetworkV4Parameters() {
        AwsNetworkV4Parameters p = new AwsNetworkV4Parameters();
        p.setSubnetId(SUBNET_ID);
        p.setVpcId(VPC_ID);
        p.setInternetGatewayId("someInternetGatewayId");
        return p;
    }

    private DetailedEnvironmentResponse createGcpEnvironment() {
        DetailedEnvironmentResponse r = new DetailedEnvironmentResponse();
        r.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        r.setNetwork(createEnvironmentNetworkResponseForGcp());
        r.setCloudPlatform("GCP");
        return r;
    }

    private DetailedEnvironmentResponse createMockEnvironment() {
        DetailedEnvironmentResponse r = new DetailedEnvironmentResponse();
        r.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        r.setNetwork(createEnvironmentNetworkResponseForMock());
        r.setCloudPlatform("MOCK");
        return r;
    }

    private EnvironmentNetworkResponse createEnvironmentNetworkResponseForMock() {
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);
        environmentNetworkResponse.setMock(createEnvironmentNetworkMockParams());
        return environmentNetworkResponse;
    }

    private EnvironmentNetworkMockParams createEnvironmentNetworkMockParams() {
        EnvironmentNetworkMockParams mockParams = new EnvironmentNetworkMockParams();
        mockParams.setVpcId("someVpcId");
        mockParams.setInternetGatewayId("someInternetGatewayId");
        return mockParams;
    }

    private EnvironmentNetworkResponse createEnvironmentNetworkResponseForGcp() {
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);
        environmentNetworkResponse.setGcp(createEnvironmentNetworkGcpParams());
        return environmentNetworkResponse;
    }

    private EnvironmentNetworkGcpParams createEnvironmentNetworkGcpParams() {
        EnvironmentNetworkGcpParams gcpParams = new EnvironmentNetworkGcpParams();
        gcpParams.setSharedProjectId("someSharedProjectId");
        gcpParams.setNoPublicIp(true);
        gcpParams.setNoFirewallRules(true);
        gcpParams.setNetworkId("someNetworkId");
        return gcpParams;
    }

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithEndpointGateway() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();
        EnvironmentNetworkResponse network = environmentNetworkResponse.getNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        CloudSubnet publicSubnet = new CloudSubnet(PUBLIC_SUBNET_ID, "name", "az-1", "cidr", false, true, true, SubnetType.PUBLIC);

        when(subnetSelector.chooseSubnetForEndpointGateway(any(), any())).thenReturn(Optional.of(publicSubnet));
        when(endpointGatewayNetworkValidator.validate(any())).thenReturn(ValidationResult.empty());

        NetworkV4Request[] networkV4Request = new NetworkV4Request[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            networkV4Request[0] = underTest
                .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
        });

        assertEquals(networkV4Request[0].createAws().getVpcId(), VPC_ID);
        assertEquals(networkV4Request[0].createAws().getSubnetId(), SUBNET_ID);
        assertEquals(networkV4Request[0].createAws().getEndpointGatewaySubnetId(), PUBLIC_SUBNET_ID);
    }

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithPrivateEndpointGateway() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();
        EnvironmentNetworkResponse network = environmentNetworkResponse.getNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        network.setEndpointGatewaySubnetIds(Set.of(SUBNET2_ID));
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(true);
        CloudSubnet privateSubnet = new CloudSubnet(SUBNET2_ID, "name", "az-1", "cidr", true, false, false, SubnetType.PRIVATE);

        when(subnetSelector.chooseSubnetForEndpointGateway(any(), any())).thenReturn(Optional.of(privateSubnet));
        when(endpointGatewayNetworkValidator.validate(any())).thenReturn(ValidationResult.empty());

        NetworkV4Request[] networkV4Request = new NetworkV4Request[1];
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            networkV4Request[0] = underTest
                    .convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
        });

        assertEquals(networkV4Request[0].createAws().getVpcId(), VPC_ID);
        assertEquals(networkV4Request[0].createAws().getSubnetId(), SUBNET_ID);
        assertEquals(networkV4Request[0].createAws().getEndpointGatewaySubnetId(), SUBNET2_ID);
    }

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithEndpointGatewayMissingSubnetNoTargeting() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();
        EnvironmentNetworkResponse network = environmentNetworkResponse.getNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);

        when(endpointGatewayNetworkValidator.validate(any())).thenReturn(ValidationResult.builder().error("error").build());

        Exception exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
            });
        });

        assertThat(exception.getMessage()).startsWith("Endpoint gateway subnet validation failed:");
    }

    @Test
    void testConvertToStackRequestWhenAwsPresentedWithEndpointGatewayWithTargetingInvalid() {
        NetworkV1Request networkV1Request = awsNetworkV1Request();
        DetailedEnvironmentResponse environmentNetworkResponse = awsEnvironmentNetwork();
        EnvironmentNetworkResponse network = environmentNetworkResponse.getNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED);
        network.setEndpointGatewaySubnetIds(Set.of(SUBNET_ID));
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(true);

        when(endpointGatewayNetworkValidator.validate(any())).thenReturn(ValidationResult.builder().error("error").build());

        Exception exception = assertThrows(BadRequestException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
                underTest.convertToNetworkV4Request(new ImmutablePair<>(networkV1Request, environmentNetworkResponse));
            });
        });

        assertThat(exception.getMessage()).startsWith("Endpoint gateway subnet validation failed:");
    }

    private DetailedEnvironmentResponse awsEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("AWS");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);

        EnvironmentNetworkAwsParams environmentNetworkAwsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkAwsParams.setVpcId(VPC_ID);

        environmentNetworkResponse.setAws(environmentNetworkAwsParams);

        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request awsNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AwsNetworkV1Parameters awsNetworkV1Parameters = new AwsNetworkV1Parameters();
        awsNetworkV1Parameters.setSubnetId(SUBNET_ID);

        networkV1Request.setAws(awsNetworkV1Parameters);

        return networkV1Request;
    }

    private NetworkV1Request awsEmptyNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AwsNetworkV1Parameters awsNetworkV1Parameters = new AwsNetworkV1Parameters();

        networkV1Request.setAws(awsNetworkV1Parameters);

        return networkV1Request;
    }

    private DetailedEnvironmentResponse azureEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("AZURE");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();
        environmentNetworkResponse.setSubnetIds(SUBNET_IDS);
        environmentNetworkResponse.setPreferedSubnetId(SUBNET_ID);

        EnvironmentNetworkAzureParams environmentNetworkAzureParams = new EnvironmentNetworkAzureParams();
        environmentNetworkAzureParams.setNetworkId(VPC_ID);
        environmentNetworkAzureParams.setResourceGroupName(GROUP_NAME);
        environmentNetworkAzureParams.setNoOutboundLoadBalancer(NO_OUTBOUND_LOAD_BALANCER);

        environmentNetworkResponse.setAzure(environmentNetworkAzureParams);

        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request azureNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AzureNetworkV1Parameters azureNetworkV1Parameters = new AzureNetworkV1Parameters();
        azureNetworkV1Parameters.setSubnetId(SUBNET_ID);

        networkV1Request.setAzure(azureNetworkV1Parameters);

        return networkV1Request;
    }

    private NetworkV1Request azureEmptyNetworkV1Request() {
        NetworkV1Request networkV1Request = new NetworkV1Request();

        AzureNetworkV1Parameters azureNetworkV1Parameters = new AzureNetworkV1Parameters();

        networkV1Request.setAzure(azureNetworkV1Parameters);

        return networkV1Request;
    }

    private DetailedEnvironmentResponse yarnEnvironmentNetwork() {
        DetailedEnvironmentResponse der = new DetailedEnvironmentResponse();
        der.setCloudPlatform("YARN");
        EnvironmentNetworkResponse environmentNetworkResponse = new EnvironmentNetworkResponse();

        EnvironmentNetworkYarnParams environmentNetwork = new EnvironmentNetworkYarnParams();
        environmentNetwork.setQueue("default");


        environmentNetworkResponse.setYarn(environmentNetwork);
        der.setNetwork(environmentNetworkResponse);
        return der;
    }

    private NetworkV1Request yarnNetworkV1Request() {
        return new NetworkV1Request();
    }

}
