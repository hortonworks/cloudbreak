package com.sequenceiq.environment.network.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.domain.EnvironmentViewConverter;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentNetworkConverterTest {

    private static final String LOCATION = "eu-west";

    private static final String NETWORK_ID = "vnet-1";

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

    private static final String RESOURCE_GROUP_NAME = "resourceGroup";

    @Mock
    private EnvironmentViewConverter environmentViewConverter;

    @InjectMocks
    private AzureEnvironmentNetworkConverter underTest = new AzureEnvironmentNetworkConverter();

    @Test
    void testConvertShouldCreateABaseNetworkFromAnEnvironmentAndANetworkDto() {
        Environment environment = createEnvironment();
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(AzureParams.AzureParamsBuilder.anAzureParams()
                        .withNetworkId(NETWORK_ID)
                        .withResourceGroupName(RESOURCE_GROUP_NAME)
                        .withNoPublicIp(true)
                        .withNoFirewallRules(true)
                        .build())
                .withName(NETWORK_NAME)
                .withNetworkCidr(NETWORK_CIDR)
                .withSubnetMetas(createSubnetMetas())
                .build();

        AzureNetwork actual = (AzureNetwork) underTest.convert(environment, networkDto, Map.of());

        assertEquals(NETWORK_NAME, actual.getName());
        assertEquals(NETWORK_ID, actual.getNetworkId());
        assertEquals(RESOURCE_GROUP_NAME, actual.getResourceGroupName());
        assertTrue(actual.getNoFirewallRules());
        assertTrue(actual.getNoPublicIp());
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

        AzureNetwork actual = (AzureNetwork) underTest.convert(environment, networkDto, Map.of());

        assertEquals(environment.getName(), actual.getName());
        assertNull(actual.getNetworkId());
        assertNull(actual.getResourceGroupName());
        assertFalse(actual.getNoPublicIp());
        assertFalse(actual.getNoFirewallRules());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(RegistrationType.CREATE_NEW, actual.getRegistrationType());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetMetas().keySet()));
        verify(environmentViewConverter).convert(environment);
    }

    @Test
    void testConvertToDtoShouldConvertABaseNetworkIntoANetworkDto() {
        AzureNetwork azureNetwork = createAzureNetwork();

        NetworkDto actual = underTest.convertToDto(azureNetwork);

        assertEquals(azureNetwork.getId(), actual.getId());
        assertEquals(azureNetwork.getName(), actual.getNetworkName());
        assertEquals(SUBNET_IDS, actual.getSubnetIds());
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_1));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_2));
        assertTrue(actual.getSubnetMetas().containsKey(SUBNET_3));
        assertEquals(azureNetwork.getNetworkCidr(), actual.getNetworkCidr());
        assertEquals(azureNetwork.getResourceCrn(), actual.getResourceCrn());
        assertEquals(azureNetwork.getNetworkId(), actual.getAzure().getNetworkId());
    }

    @Test
    void testSetProviderSpecificNetworkShouldPopulateTheExistingNetworkWithTheNewNetworkData() {
        AzureNetwork azureNetwork = new AzureNetwork();
        Set<CreatedSubnet> createdSubnets = createCreatedSubnets();
        Map<String, Object> properties = Map.of("resourceGroupName", RESOURCE_GROUP_NAME);
        CreatedCloudNetwork createdCloudNetwork = new CreatedCloudNetwork("network-1", NETWORK_ID, createdSubnets, properties);

        AzureNetwork actual = (AzureNetwork) underTest.setCreatedCloudNetwork(azureNetwork, createdCloudNetwork);

        assertEquals(createdCloudNetwork.getStackName(), actual.getName());
        assertEquals(NETWORK_ID, actual.getNetworkId());
        assertEquals(RESOURCE_GROUP_NAME, actual.getResourceGroupName());
        assertTrue(SUBNET_IDS.containsAll(actual.getSubnetMetas().keySet()));

        assertEquals(SUBNET_1, actual.getSubnetMetas().get(SUBNET_1).getId());
        assertEquals(SUBNET_1, actual.getSubnetMetas().get(SUBNET_1).getName());
        assertEquals(AZ_1, actual.getSubnetMetas().get(SUBNET_1).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_1, actual.getSubnetMetas().get(SUBNET_1).getCidr());
        assertTrue(actual.getSubnetMetas().get(SUBNET_1).isPrivateSubnet());

        assertEquals(SUBNET_2, actual.getSubnetMetas().get(SUBNET_2).getId());
        assertEquals(SUBNET_2, actual.getSubnetMetas().get(SUBNET_2).getName());
        assertEquals(AZ_2, actual.getSubnetMetas().get(SUBNET_2).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_2, actual.getSubnetMetas().get(SUBNET_2).getCidr());
        assertTrue(actual.getSubnetMetas().get(SUBNET_2).isPrivateSubnet());

        assertEquals(SUBNET_3, actual.getSubnetMetas().get(SUBNET_3).getId());
        assertEquals(SUBNET_3, actual.getSubnetMetas().get(SUBNET_3).getName());
        assertEquals(AZ_3, actual.getSubnetMetas().get(SUBNET_3).getAvailabilityZone());
        assertEquals(SUBNET_CIDR_3, actual.getSubnetMetas().get(SUBNET_3).getCidr());
        assertTrue(actual.getSubnetMetas().get(SUBNET_3).isPrivateSubnet());
    }

    @Test
    void testConvertToNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(NETWORK_ID);
        azureNetwork.setResourceGroupName(RESOURCE_GROUP_NAME);

        Network network = underTest.convertToNetwork(azureNetwork);

        assertEquals(RESOURCE_GROUP_NAME, network.getStringParameter(AzureUtils.RG_NAME));
        assertEquals(NETWORK_ID, network.getStringParameter(AzureUtils.NETWORK_ID));
    }

    private Set<CreatedSubnet> createCreatedSubnets() {
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setSubnetId(SUBNET_1);
        createdSubnet1.setAvailabilityZone(AZ_1);
        createdSubnet1.setCidr(SUBNET_CIDR_1);
        createdSubnet1.setPublicSubnet(true);

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setSubnetId(SUBNET_2);
        createdSubnet2.setAvailabilityZone(AZ_2);
        createdSubnet2.setCidr(SUBNET_CIDR_2);
        createdSubnet2.setPublicSubnet(true);

        CreatedSubnet createdSubnet3 = new CreatedSubnet();
        createdSubnet3.setSubnetId(SUBNET_3);
        createdSubnet3.setAvailabilityZone(AZ_3);
        createdSubnet3.setCidr(SUBNET_CIDR_3);
        createdSubnet3.setPublicSubnet(true);
        return Set.of(createdSubnet1, createdSubnet2, createdSubnet3);
    }

    private AzureNetwork createAzureNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setId(1L);
        azureNetwork.setName("network-1");
        azureNetwork.setSubnetMetas(createSubnetMetas());
        azureNetwork.setNetworkCidr(NETWORK_CIDR);
        azureNetwork.setResourceCrn("crn");
        azureNetwork.setNetworkId(NETWORK_ID);
        azureNetwork.setResourceGroupName(RESOURCE_GROUP_NAME);
        azureNetwork.setNoFirewallRules(true);
        azureNetwork.setNoPublicIp(true);
        return azureNetwork;
    }

    private Map<String, CloudSubnet> createSubnetMetas() {
        return Map.of(SUBNET_1, new CloudSubnet(), SUBNET_2, new CloudSubnet(), SUBNET_3, new CloudSubnet());
    }

    private Environment createEnvironment() {
        Environment environment = new Environment();
        environment.setName(ENV_NAME);
        environment.setId(1L);
        environment.setAccountId("2");
        environment.setDescription("description");
        environment.setCloudPlatform("AZURE");
        environment.setCredential(new Credential());
        environment.setLatitude(2.4);
        environment.setLongitude(3.5);
        environment.setLocation(LOCATION);
        environment.setLocationDisplayName("London");
        environment.setNetwork(new AzureNetwork());
        environment.setRegions(Collections.singleton(new Region()));
        return environment;
    }
}
