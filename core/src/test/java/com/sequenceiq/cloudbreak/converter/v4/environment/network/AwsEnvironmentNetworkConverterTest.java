package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAwsV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.cloud.model.network.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.environment.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.RegistrationType;
import com.sequenceiq.cloudbreak.domain.environment.Subnet;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class AwsEnvironmentNetworkConverterTest {

    private static final String VPC_ID = "vpc-id";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final Set<String> SUBNET_IDS = Set.of(SUBNET_1, SUBNET_2);

    private static final String SUBNET_IDS_STRING = "subnet-1,subnet-2";

    private static final String SUBNET_CIDRS_STRING = "1.1.1.1/8,2.2.2.2/8";

    private static final String NETWORK_CIDR = "0.0.0.0/16";

    private static final String ENV_NAME = "TEST_ENV";

    private static final Workspace WORKSPACE = new Workspace();

    private static final Long NETWORK_ID = 1L;

    @InjectMocks
    private AwsEnvironmentNetworkConverter underTest;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private SubnetConverter subnetConverter;

    @Test
    public void testConvertShouldConvertAnEnvironmentNetworkV4RequestAndAnEnvironmentIntoABaseNetwork() {
        EnvironmentNetworkV4Request request = createRequest();
        Environment environment = createEnvironment();

        AwsNetwork actual = (AwsNetwork) underTest.convert(request, environment);

        assertEquals(ENV_NAME, actual.getName());
        assertEquals(environment, actual.getEnvironment());
        assertEquals(WORKSPACE, actual.getWorkspace());
        assertTrue(actual.getSubnetSet().stream().anyMatch(subnet -> subnet.getSubnetId().equals(SUBNET_1)));
        assertTrue(actual.getSubnetSet().stream().anyMatch(subnet -> subnet.getSubnetId().equals(SUBNET_2)));
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.EXISTING, actual.getRegistrationType());
        assertEquals(VPC_ID, actual.getVpcId());
    }

    @Test
    public void testConvertNewNetworkShouldCreateANewNetworkBasedOnTheParameters() {
        EnvironmentNetworkV4Request request = createRequest();
        Environment environment = createEnvironment();
        CreatedCloudNetwork createdCloudNetwork = createCreatedCloudNetwork();
        Set<Subnet> subnets = createSubnets();

        when(subnetConverter.convert(createdCloudNetwork.getSubnets())).thenReturn(subnets);

        AwsNetwork actual = (AwsNetwork) underTest.convertNewNetwork(request, environment, createdCloudNetwork);

        verify(subnetConverter).convert(createdCloudNetwork.getSubnets());
        assertEquals(ENV_NAME, actual.getName());
        assertEquals(environment, actual.getEnvironment());
        assertEquals(WORKSPACE, actual.getWorkspace());
        assertEquals(subnets.size(), actual.getSubnetSet().size());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.CREATE_NEW, actual.getRegistrationType());
        assertEquals(VPC_ID, actual.getVpcId());
    }

    @Test
    public void testConvertShouldConvertABaseNetworkIntoAnEnvironmentNetworkV4Response() {
        BaseNetwork baseNetwork = createBaseNetwork();

        EnvironmentNetworkV4Response actual = underTest.convert(baseNetwork);

        assertEquals(NETWORK_ID, actual.getId());
        assertEquals(ENV_NAME, actual.getName());
        assertEquals(SUBNET_IDS, actual.getSubnetIds());
        assertEquals(VPC_ID, actual.getAws().getVpcId());
    }

    @Test
    public void testConvertToLegacyNetworkShouldConvertABaseNetworkIntoANetwork() {
        BaseNetwork baseNetwork = createBaseNetwork();
        when(missingResourceNameGenerator.generateName(APIResourceType.NETWORK)).thenReturn(ENV_NAME);

        Network actual = underTest.convertToLegacyNetwork(baseNetwork);

        verify(missingResourceNameGenerator).generateName(APIResourceType.NETWORK);
        assertEquals(ENV_NAME, actual.getName());
        assertNull(actual.getSubnetCIDR());
        assertEquals(SUBNET_IDS_STRING, actual.getAttributes().getMap().get("subnetId"));
        assertEquals(SUBNET_CIDRS_STRING, actual.getAttributes().getMap().get("subnetCidrs"));
        assertEquals(CloudPlatform.AWS.toString(), actual.getAttributes().getMap().get("cloudPlatform"));
        assertEquals(VPC_ID, actual.getAttributes().getMap().get("vpcId"));
    }

    private EnvironmentNetworkV4Request createRequest() {
        EnvironmentNetworkV4Request request = new EnvironmentNetworkV4Request();
        request.setSubnetIds(SUBNET_IDS);
        request.setNetworkCidr(NETWORK_CIDR);
        request.setAws(createAwsV4Params());
        return request;
    }

    private EnvironmentNetworkAwsV4Params createAwsV4Params() {
        EnvironmentNetworkAwsV4Params awsV4Params = new EnvironmentNetworkAwsV4Params();
        awsV4Params.setVpcId(VPC_ID);
        return awsV4Params;
    }

    private Environment createEnvironment() {
        Environment environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setWorkspace(WORKSPACE);
        return environment;
    }

    private CreatedCloudNetwork createCreatedCloudNetwork() {
        return new CreatedCloudNetwork(VPC_ID, createCloudSubnets());
    }

    private BaseNetwork createBaseNetwork() {
        AwsNetwork baseNetwork = new AwsNetwork();
        ReflectionTestUtils.setField(baseNetwork, "id", NETWORK_ID);
        baseNetwork.setName(ENV_NAME);
        baseNetwork.setSubnets(createSubnets());
        baseNetwork.setVpcId(VPC_ID);
        return baseNetwork;
    }

    private Set<CloudSubnet> createCloudSubnets() {
        return Set.of(new CloudSubnet());
    }

    private Set<Subnet> createSubnets() {
        Subnet subnet1 = new Subnet();
        subnet1.setSubnetId(SUBNET_1);
        subnet1.setCidr("1.1.1.1/8");

        Subnet subnet2 = new Subnet();
        subnet2.setSubnetId(SUBNET_2);
        subnet2.setCidr("2.2.2.2/8");

        return Set.of(subnet1, subnet2);
    }
}