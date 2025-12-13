package com.sequenceiq.environment.network.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.DeploymentRestriction;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AwsEnvironmentNetworkConverterTest {

    private static final String LOCATION = "eu-west";

    private static final String VPC_ID = "vpc-1";

    private static final String NETWORK_NAME = "network-1";

    private static final String NETWORK_CIDR = "1.1.1.1/16";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    private static final String PUBLIC_SUBNET_1 = "public-subnet-1";

    private static final String PUBLIC_SUBNET_2 = "public-subnet-2";

    private static final String PRIVATE_SUBNET_1 = SUBNET_1 + "-private";

    private static final String PRIVATE_SUBNET_2 = SUBNET_2 + "-private";

    private static final String PRIVATE_SUBNET_3 = SUBNET_3 + "-private";

    private static final String PUBLIC_SUBNET_3 = "public-subnet-3";

    private static final Set<String> SUBNET_IDS = Set.of(SUBNET_1, SUBNET_2, SUBNET_3);

    private static final Set<String> PUBLIC_SUBNET_IDS = Set.of(PUBLIC_SUBNET_1, PUBLIC_SUBNET_2, PUBLIC_SUBNET_3);

    private static final String ENV_NAME = "testEnv";

    private static final String AZ_1 = "az-1";

    private static final String AZ_2 = "az-2";

    private static final String AZ_3 = "az-3";

    private static final String SUBNET_CIDR_1 = "1.1.1.1/24";

    private static final String SUBNET_CIDR_3 = "3.3.3.3/24";

    private static final String SUBNET_CIDR_2 = "2.2.2.2/24";

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private AwsEnvironmentNetworkConverter underTest;

    @Test
    void testConvertShouldCreateABaseNetworkFromAnEnvironmentAndANetworkDto() {
        Environment environment = createEnvironment();
        NetworkDto networkDto = NetworkDto.builder()
                .withAws(AwsParams.builder().withVpcId(VPC_ID).build())
                .withName(NETWORK_NAME)
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetMetas(createSubnetMetas())
                .build();

        AwsNetwork actual = (AwsNetwork) underTest.convert(environment, networkDto, Map.of(), Map.of());

        assertEquals(NETWORK_NAME, actual.getName());
        assertEquals(VPC_ID, actual.getVpcId());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.EXISTING, actual.getRegistrationType());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetMetas().keySet()));
        verify(environmentViewConverter).convert(environment);
    }

    @Test
    void testConvertShouldCreateABaseNetworkFromAnEnvironmentAndANetworkDtoWhenOptionalFieldsAreNotPresent() {
        Environment environment = createEnvironment();
        NetworkDto networkDto = NetworkDto.builder()
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetMetas(createSubnetMetas())
                .build();

        AwsNetwork actual = (AwsNetwork) underTest.convert(environment, networkDto, Map.of(), Map.of());

        assertEquals(environment.getName(), actual.getName());
        assertNull(actual.getVpcId());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.CREATE_NEW, actual.getRegistrationType());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetMetas().keySet()));
        verify(environmentViewConverter).convert(environment);
    }

    @Test
    void testConvertToDtoShouldConvertABaseNetworkIntoANetworkDto() {
        AwsNetwork awsNetwork = createAwsNetwork();

        NetworkDto actual = underTest.convertToDto(awsNetwork);

        assertEquals(awsNetwork.getId(), actual.getId());
        assertEquals(awsNetwork.getName(), actual.getNetworkName());
        assertEquals(SUBNET_IDS, actual.getSubnetIds());
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_1));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_2));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_3));
        assertNull(actual.getPublicEndpointAccessGateway());
        assertEquals(0, actual.getEndpointGatewaySubnetIds().size());
        assertEquals(awsNetwork.getNetworkCidr(), actual.getNetworkCidr());
        assertEquals(awsNetwork.getResourceCrn(), actual.getResourceCrn());
        assertEquals(awsNetwork.getVpcId(), actual.getAws().getVpcId());
    }

    @Test
    void testConvertToDtoShouldLeaveExistingDeploymetRestrictionsIntact() {
        AwsNetwork awsNetwork = createAwsNetwork();
        Set<DeploymentRestriction> liftie = Set.of(DeploymentRestriction.LIFTIE);
        awsNetwork.setSubnetMetas(awsNetwork.getSubnetMetas().entrySet().stream()
                .peek(e -> e.getValue().setDeploymentRestrictions(liftie))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        NetworkDto actual = underTest.convertToDto(awsNetwork);

        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_1));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_2));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_3));
        assertThat(actual.getSubnetMetas().values()).allMatch(subnet -> liftie.equals(subnet.getDeploymentRestrictions()));
    }

    @Test
    void testConvertToDtoShouldConvertABaseNetworkWithEndpointSubnetsIntoANetworkDto() {
        AwsNetwork awsNetwork = createAwsNetwork();
        awsNetwork.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        awsNetwork.setEndpointGatewaySubnetMetas(createEndpointSubnetMetas());

        NetworkDto actual = underTest.convertToDto(awsNetwork);

        assertEquals(awsNetwork.getId(), actual.getId());
        assertEquals(awsNetwork.getName(), actual.getNetworkName());
        assertEquals(SUBNET_IDS, actual.getSubnetIds());
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_1));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_2));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_3));
        assertEquals(PublicEndpointAccessGateway.ENABLED, actual.getPublicEndpointAccessGateway());
        assertEquals(PUBLIC_SUBNET_IDS, actual.getEndpointGatewaySubnetIds());
        assertTrue(actual.getEndpointGatewaySubnetMetas().containsKey(PUBLIC_SUBNET_1));
        assertTrue(actual.getEndpointGatewaySubnetMetas().containsKey(PUBLIC_SUBNET_2));
        assertTrue(actual.getEndpointGatewaySubnetMetas().containsKey(PUBLIC_SUBNET_3));
        assertEquals(awsNetwork.getNetworkCidr(), actual.getNetworkCidr());
        assertEquals(awsNetwork.getResourceCrn(), actual.getResourceCrn());
        assertEquals(awsNetwork.getVpcId(), actual.getAws().getVpcId());
    }

    @Test
    void testSetProviderSpecificNetworkWithOnlyPublicShouldPopulateTheExistingNetworkWithTheNewNetworkData() {
        BaseNetwork awsNetwork = new AwsNetwork();
        Set<CreatedSubnet> createdSubnets = createCreatedPublicSubnets();
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork("network-1", VPC_ID, createdSubnets);

        AwsNetwork actual = (AwsNetwork) underTest.setCreatedCloudNetwork(awsNetwork, createdCloudNetwork);

        assertEquals(createdCloudNetwork.getStackName(), actual.getName());
        assertEquals(VPC_ID, actual.getVpcId());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetMetas().keySet()));

        assertEquals(SUBNET_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getId());
        assertEquals(SUBNET_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getName());
        assertEquals(AZ_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_1).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_1).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);

        assertEquals(SUBNET_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getId());
        assertEquals(SUBNET_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getName());
        assertEquals(AZ_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_2).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_3).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);

        assertEquals(SUBNET_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getId());
        assertEquals(SUBNET_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getName());
        assertEquals(AZ_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_3).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_3).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);
    }

    @Test
    void testSetProviderSpecificNetworkWithPrivateAndPublicShouldPopulateTheExistingNetworkWithTheNewNetworkData() {
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        BaseNetwork awsNetwork = new AwsNetwork();
        Set<CreatedSubnet> createdSubnets = createCreatedPublicSubnets();
        createdSubnets.addAll(createCreatedPrivateSubnets());
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork("network-1", VPC_ID, createdSubnets);

        AwsNetwork actual = (AwsNetwork) ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1",
            () -> underTest.setCreatedCloudNetwork(awsNetwork, createdCloudNetwork));
        assertEquals(createdCloudNetwork.getStackName(), actual.getName());
        assertEquals(VPC_ID, actual.getVpcId());
        Set<String> subnetSet = new HashSet<>();
        subnetSet.addAll(SUBNET_IDS);
        subnetSet.add(PRIVATE_SUBNET_1);
        subnetSet.add(PRIVATE_SUBNET_2);
        subnetSet.add(PRIVATE_SUBNET_3);
        assertTrue(subnetSet.containsAll(actual.getSubnetMetas().keySet()));

        assertEquals(SUBNET_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getId());
        assertEquals(SUBNET_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getName());
        assertEquals(AZ_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_1, awsNetwork.getSubnetMetas().get(SUBNET_1).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_1).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_1).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);

        assertEquals(PRIVATE_SUBNET_1, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).getId());
        assertEquals(PRIVATE_SUBNET_1, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).getName());
        assertEquals(AZ_1, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_1, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).getCidr());
        assertTrue(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_1).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);

        assertEquals(SUBNET_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getId());
        assertEquals(SUBNET_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getName());
        assertEquals(AZ_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_2, awsNetwork.getSubnetMetas().get(SUBNET_2).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_2).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_3).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);

        assertEquals(PRIVATE_SUBNET_2, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).getId());
        assertEquals(PRIVATE_SUBNET_2, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).getName());
        assertEquals(AZ_2, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_2, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).getCidr());
        assertTrue(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_2).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);

        assertEquals(SUBNET_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getId());
        assertEquals(SUBNET_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getName());
        assertEquals(AZ_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_3, awsNetwork.getSubnetMetas().get(SUBNET_3).getCidr());
        assertFalse(awsNetwork.getSubnetMetas().get(SUBNET_3).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(SUBNET_3).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ENDPOINT_ACCESS_GATEWAYS);

        assertEquals(PRIVATE_SUBNET_3, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).getId());
        assertEquals(PRIVATE_SUBNET_3, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).getName());
        assertEquals(AZ_3, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_3, awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).getCidr());
        assertTrue(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).isPrivateSubnet());
        assertThat(awsNetwork.getSubnetMetas().get(PRIVATE_SUBNET_3).getDeploymentRestrictions())
                .containsAll(DeploymentRestriction.ALL);
    }

    @Test
    void testConvertToNetwork() {
        EnvironmentView environment = new EnvironmentView();
        environment.setLocation(LOCATION);
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setVpcId(VPC_ID);
        awsNetwork.setEnvironments(Set.of(environment));

        Network network = underTest.convertToNetwork(awsNetwork);

        assertEquals(VPC_ID, network.getStringParameter(NetworkConstants.VPC_ID));
    }

    private Set<CreatedSubnet> createCreatedPublicSubnets() {
        Set<CreatedSubnet> result = new HashSet<>();
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setSubnetId(SUBNET_1);
        createdSubnet1.setAvailabilityZone(AZ_1);
        createdSubnet1.setCidr(SUBNET_CIDR_1);
        createdSubnet1.setPublicSubnet(true);
        result.add(createdSubnet1);

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setSubnetId(SUBNET_2);
        createdSubnet2.setAvailabilityZone(AZ_2);
        createdSubnet2.setCidr(SUBNET_CIDR_2);
        createdSubnet2.setPublicSubnet(true);
        result.add(createdSubnet2);

        CreatedSubnet createdSubnet3 = new CreatedSubnet();
        createdSubnet3.setSubnetId(SUBNET_3);
        createdSubnet3.setAvailabilityZone(AZ_3);
        createdSubnet3.setCidr(SUBNET_CIDR_3);
        createdSubnet3.setPublicSubnet(true);
        result.add(createdSubnet3);

        return result;
    }

    private Set<CreatedSubnet> createCreatedPrivateSubnets() {
        Set<CreatedSubnet> result = new HashSet<>();
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setSubnetId(PRIVATE_SUBNET_1);
        createdSubnet1.setAvailabilityZone(AZ_1);
        createdSubnet1.setCidr(SUBNET_CIDR_1);
        createdSubnet1.setPublicSubnet(false);
        result.add(createdSubnet1);

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setSubnetId(PRIVATE_SUBNET_2);
        createdSubnet2.setAvailabilityZone(AZ_2);
        createdSubnet2.setCidr(SUBNET_CIDR_2);
        createdSubnet2.setPublicSubnet(false);
        result.add(createdSubnet2);

        CreatedSubnet createdSubnet3 = new CreatedSubnet();
        createdSubnet3.setSubnetId(PRIVATE_SUBNET_3);
        createdSubnet3.setAvailabilityZone(AZ_3);
        createdSubnet3.setCidr(SUBNET_CIDR_3);
        createdSubnet3.setPublicSubnet(false);
        result.add(createdSubnet3);

        return result;
    }

    private AwsNetwork createAwsNetwork() {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setId(1L);
        awsNetwork.setName("network-1");
        awsNetwork.setSubnetMetas(createSubnetMetas());
        awsNetwork.setNetworkCidr(NETWORK_CIDR);
        awsNetwork.setResourceCrn("crn");
        awsNetwork.setVpcId(VPC_ID);
        return awsNetwork;
    }

    private Map<String, CloudSubnet> createSubnetMetas() {
        return Map.of(SUBNET_1, new CloudSubnet(), SUBNET_2, new CloudSubnet(), SUBNET_3, new CloudSubnet());
    }

    private Map<String, CloudSubnet> createEndpointSubnetMetas() {
        return Map.of(PUBLIC_SUBNET_1, new CloudSubnet(), PUBLIC_SUBNET_2, new CloudSubnet(), PUBLIC_SUBNET_3, new CloudSubnet());
    }

    private Environment createEnvironment() {
        Environment environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setId(1L);
        environment.setAccountId("2");
        environment.setDescription("description");
        environment.setCloudPlatform("AWS");
        environment.setCredential(new Credential());
        environment.setLatitude(2.4);
        environment.setLongitude(3.5);
        environment.setLocation(LOCATION);
        environment.setLocationDisplayName("London");
        environment.setNetwork(new AwsNetwork());
        environment.setRegions(Collections.singleton(new Region()));
        return environment;
    }

    @Test
    public void testGetNetworkCidrWhenDuplicated() {
        AwsNetwork awsNetwork = createAwsNetwork();
        awsNetwork.setNetworkCidrs("10.0.0.0/16,10.0.0.0/16");
        Set<String> actual = underTest.getNetworkCidrs(awsNetwork);
        assertEquals(1, actual.size());
        assertEquals("10.0.0.0/16", actual.iterator().next());
    }

}
