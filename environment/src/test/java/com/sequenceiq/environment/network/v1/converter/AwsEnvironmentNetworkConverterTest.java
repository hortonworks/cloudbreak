package com.sequenceiq.environment.network.v1.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@RunWith(MockitoJUnitRunner.class)
public class AwsEnvironmentNetworkConverterTest {

    private static final String LOCATION = "eu-west";

    private static final String VPC_ID = "vpc-1";

    private static final String NETWORK_NAME = "network-1";

    private static final String NETWORK_CIDR = "1.1.1.1/16";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    private static final Set<String> SUBNET_IDS = Set.of(SUBNET_1, SUBNET_2, SUBNET_3);

    private static final String ENV_NAME = "testEnv";

    private static final String AZ_1 = "az-1";

    private static final String AZ_2 = "az-2";

    private static final String AZ_3 = "az-3";

    private static final String SUBNET_CIDR_1 = "1.1.1.1/24";

    private static final String SUBNET_CIDR_3 = "3.3.3.3/24";

    private static final String SUBNET_CIDR_2 = "2.2.2.2/24";

    private AwsEnvironmentNetworkConverter underTest = new AwsEnvironmentNetworkConverter();

    @Test
    public void testConvertShouldCreateABaseNetworkFromAnEnvironmentAndANetworkDto() {
        Environment environment = createEnvironment();
        NetworkDto networkDto = NetworkDto.Builder.aNetworkDto()
                .withAws(AwsParams.AwsParamsBuilder.anAwsParams().withVpcId(VPC_ID).build())
                .withName(NETWORK_NAME)
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetIds(SUBNET_IDS)
                .build();

        AwsNetwork actual = (AwsNetwork) underTest.convert(environment, networkDto);

        assertEquals(NETWORK_NAME, actual.getName());
        assertEquals(VPC_ID, actual.getVpcId());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.EXISTING, actual.getRegistrationType());
        assertEquals(SUBNET_IDS, actual.getSubnetIdsSet());
        assertEquals(SUBNET_1, actual.getSubnetMetasMap().get(SUBNET_1).getId());
        assertEquals(SUBNET_2, actual.getSubnetMetasMap().get(SUBNET_2).getId());
        assertEquals(SUBNET_3, actual.getSubnetMetasMap().get(SUBNET_3).getId());
        assertEnvironmentView(environment, actual);
    }

    @Test
    public void testConvertShouldCreateABaseNetworkFromAnEnvironmentAndANetworkDtoWhenOptionalFieldsAreNotPresent() {
        Environment environment = createEnvironment();
        NetworkDto networkDto = NetworkDto.Builder.aNetworkDto()
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetIds(SUBNET_IDS)
                .build();

        AwsNetwork actual = (AwsNetwork) underTest.convert(environment, networkDto);

        assertEquals(environment.getName(), actual.getName());
        assertNull(actual.getVpcId());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.CREATE_NEW, actual.getRegistrationType());
        assertEquals(SUBNET_IDS, actual.getSubnetIdsSet());
        assertEquals(SUBNET_1, actual.getSubnetMetasMap().get(SUBNET_1).getId());
        assertEquals(SUBNET_2, actual.getSubnetMetasMap().get(SUBNET_2).getId());
        assertEquals(SUBNET_3, actual.getSubnetMetasMap().get(SUBNET_3).getId());
        assertEnvironmentView(environment, actual);
    }

    @Test
    public void testConvertToDtoShouldConvertABaseNetworkIntoANetworkDto() {
        AwsNetwork awsNetwork = createAwsNetwork();

        NetworkDto actual = underTest.convertToDto(awsNetwork);

        assertEquals(awsNetwork.getId(), actual.getId());
        assertEquals(awsNetwork.getName(), actual.getNetworkName());
        assertEquals(SUBNET_IDS, actual.getSubnetIds());
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_1));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_2));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_3));
        assertEquals(awsNetwork.getNetworkCidr(), actual.getNetworkCidr());
        assertEquals(awsNetwork.getResourceCrn(), actual.getResourceCrn());
        assertEquals(awsNetwork.getVpcId(), actual.getAws().getVpcId());
    }

    @Test
    public void testSetProviderSpecificNetworkShouldPopulateTheExistingNetworkWithTheNewNetworkData() {
        BaseNetwork awsNetwork = new AwsNetwork();
        Set<CreatedSubnet> createdSubnets = createCreatedSubnets();
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork("network-1", VPC_ID, createdSubnets);

        AwsNetwork actual = (AwsNetwork) underTest.setProviderSpecificNetwork(awsNetwork, createdCloudNetwork);

        assertEquals(createdCloudNetwork.getStackName(), actual.getName());
        assertEquals(RegistrationType.CREATE_NEW, actual.getRegistrationType());
        assertEquals(VPC_ID, actual.getVpcId());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetIdsSet()));

        assertEquals(SUBNET_1, awsNetwork.getSubnetMetasMap().get(SUBNET_1).getId());
        assertEquals(SUBNET_1, awsNetwork.getSubnetMetasMap().get(SUBNET_1).getName());
        assertEquals(AZ_1, awsNetwork.getSubnetMetasMap().get(SUBNET_1).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_1, awsNetwork.getSubnetMetasMap().get(SUBNET_1).getCidr());
        assertTrue(awsNetwork.getSubnetMetasMap().get(SUBNET_1).isPrivateSubnet());

        assertEquals(SUBNET_2, awsNetwork.getSubnetMetasMap().get(SUBNET_2).getId());
        assertEquals(SUBNET_2, awsNetwork.getSubnetMetasMap().get(SUBNET_2).getName());
        assertEquals(AZ_2, awsNetwork.getSubnetMetasMap().get(SUBNET_2).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_2, awsNetwork.getSubnetMetasMap().get(SUBNET_2).getCidr());
        assertTrue(awsNetwork.getSubnetMetasMap().get(SUBNET_2).isPrivateSubnet());

        assertEquals(SUBNET_3, awsNetwork.getSubnetMetasMap().get(SUBNET_3).getId());
        assertEquals(SUBNET_3, awsNetwork.getSubnetMetasMap().get(SUBNET_3).getName());
        assertEquals(AZ_3, awsNetwork.getSubnetMetasMap().get(SUBNET_3).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_3, awsNetwork.getSubnetMetasMap().get(SUBNET_3).getCidr());
        assertTrue(awsNetwork.getSubnetMetasMap().get(SUBNET_3).isPrivateSubnet());
    }

    private Set<CreatedSubnet> createCreatedSubnets() {
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setSubnetId(SUBNET_1);
        createdSubnet1.setAvailabilityZone(AZ_1);
        createdSubnet1.setCidr(SUBNET_CIDR_1);
        createdSubnet1.setPrivateSubnet(true);

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setSubnetId(SUBNET_2);
        createdSubnet2.setAvailabilityZone(AZ_2);
        createdSubnet2.setCidr(SUBNET_CIDR_2);
        createdSubnet2.setPrivateSubnet(true);

        CreatedSubnet createdSubnet3 = new CreatedSubnet();
        createdSubnet3.setSubnetId(SUBNET_3);
        createdSubnet3.setAvailabilityZone(AZ_3);
        createdSubnet3.setCidr(SUBNET_CIDR_3);
        createdSubnet3.setPrivateSubnet(true);
        return Set.of(createdSubnet1, createdSubnet2, createdSubnet3);
    }

    private AwsNetwork createAwsNetwork() {
        AwsNetwork awsNetwork = new AwsNetwork();
        awsNetwork.setId(1L);
        awsNetwork.setName("network-1");
        awsNetwork.setSubnetMetas(Map.of(SUBNET_1, new CloudSubnet(), SUBNET_2, new CloudSubnet(), SUBNET_3, new CloudSubnet()));
        awsNetwork.setNetworkCidr(NETWORK_CIDR);
        awsNetwork.setResourceCrn("crn");
        awsNetwork.setVpcId(VPC_ID);
        return awsNetwork;
    }

    private void assertEnvironmentView(Environment environment, AwsNetwork actual) {
        EnvironmentView actualView = actual.getEnvironments().iterator().next();
        assertEquals(environment.getId(), actualView.getId());
        assertEquals(environment.getName(), actualView.getName());
        assertEquals(environment.getAccountId(), actualView.getAccountId());
        assertEquals(environment.getDescription(), actualView.getDescription());
        assertEquals(environment.getCloudPlatform(), actualView.getCloudPlatform());
        assertEquals(environment.getCredential(), actualView.getCredential());
        assertEquals(environment.getLatitude(), actualView.getLatitude());
        assertEquals(environment.getLongitude(), actualView.getLongitude());
        assertEquals(environment.getLocation(), actualView.getLocation());
        assertEquals(environment.getLocationDisplayName(), actualView.getLocationDisplayName());
        assertEquals(environment.getNetwork(), actualView.getNetwork());
        assertEquals(environment.getRegions(), actualView.getRegions());
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

}