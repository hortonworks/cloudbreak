package com.sequenceiq.environment.environment.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.base.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.MockParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;
import com.sequenceiq.environment.network.service.SubnetIdProvider;

@ExtendWith(SpringExtension.class)
public class NetworkDtoToResponseConverterTest {

    private static final Tunnel TUNNEL = Tunnel.CCM;

    private static final String PREFERRED_SUBNET_ID = "preferred-subnet-id";

    @InjectMocks
    private NetworkDtoToResponseConverter underTest;

    @Mock
    private SubnetIdProvider subnetIdProvider;

    @Test
    void testConvertWithAwsParams() {
        NetworkDto network = createNetworkDto().withAws(createAwsParams()).build();
        EnvironmentDto environmentDto = createEnvironmentDto(network);
        when(subnetIdProvider.provide(environmentDto)).thenReturn(PREFERRED_SUBNET_ID);

        EnvironmentNetworkResponse actual = underTest.convert(environmentDto);

        assertCommonFields(environmentDto, actual);
        assertEquals(network.getAws().getVpcId(), actual.getAws().getVpcId());
        assertNull(actual.getAzure());
        assertNull(actual.getYarn());
        assertNull(actual.getMock());
    }

    @Test
    void testConvertWithAzureParams() {
        NetworkDto network = createNetworkDto().withAzure(createAzureParams()).build();
        EnvironmentDto environmentDto = createEnvironmentDto(network);
        when(subnetIdProvider.provide(environmentDto)).thenReturn(PREFERRED_SUBNET_ID);

        EnvironmentNetworkResponse actual = underTest.convert(environmentDto);

        assertCommonFields(environmentDto, actual);
        assertEquals(network.getAzure().isNoPublicIp(), actual.getAzure().getNoPublicIp());
        assertEquals(network.getAzure().getNetworkId(), actual.getAzure().getNetworkId());
        assertEquals(network.getAzure().getResourceGroupName(), actual.getAzure().getResourceGroupName());
        assertNull(actual.getAws());
        assertNull(actual.getYarn());
        assertNull(actual.getMock());
    }

    @Test
    void testConvertWithMockParams() {
        NetworkDto network = createNetworkDto().withMock(createMockParams()).build();
        EnvironmentDto environmentDto = createEnvironmentDto(network);
        when(subnetIdProvider.provide(environmentDto)).thenReturn(PREFERRED_SUBNET_ID);

        EnvironmentNetworkResponse actual = underTest.convert(environmentDto);

        assertCommonFields(environmentDto, actual);
        assertEquals(network.getMock().getVpcId(), actual.getMock().getVpcId());
        assertEquals(network.getMock().getInternetGatewayId(), actual.getMock().getInternetGatewayId());
        assertNull(actual.getAws());
        assertNull(actual.getYarn());
        assertNull(actual.getAzure());
    }

    @Test
    void testConvertWithYarnParams() {
        NetworkDto network = createNetworkDto().withYarn(createYarnParams()).build();
        EnvironmentDto environmentDto = createEnvironmentDto(network);

        when(subnetIdProvider.provide(environmentDto)).thenReturn(PREFERRED_SUBNET_ID);

        EnvironmentNetworkResponse actual = underTest.convert(environmentDto);

        assertCommonFields(environmentDto, actual);
        assertEquals(network.getYarn().getQueue(), actual.getYarn().getQueue());
        assertNull(actual.getAws());
        assertNull(actual.getMock());
        assertNull(actual.getAzure());
    }

    private EnvironmentDto createEnvironmentDto(NetworkDto networkDto) {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        environmentDto.setExperimentalFeatures(ExperimentalFeatures.builder().withTunnel(TUNNEL).build());
        environmentDto.setCloudPlatform(networkDto.getCloudPlatform().toString());
        return environmentDto;
    }

    private void assertCommonFields(EnvironmentDto environmentDto, EnvironmentNetworkResponse actual) {
        verify(subnetIdProvider).provide(environmentDto);
        NetworkDto network = environmentDto.getNetwork();
        assertEquals(network.getResourceCrn(), actual.getCrn());
        assertEquals(network.getSubnetIds(), actual.getSubnetIds());
        assertEquals(network.getNetworkCidr(), actual.getNetworkCidr());
        assertEquals(network.getSubnetMetas(), actual.getSubnetMetas());
        assertEquals(network.getCbSubnets(), actual.getCbSubnets());
        assertEquals(network.getDwxSubnets(), actual.getDwxSubnets());
        assertEquals(network.getMlxSubnets(), actual.getMlxSubnets());
        assertEquals(PREFERRED_SUBNET_ID, actual.getPreferedSubnetId());
        assertEquals(network.getPrivateSubnetCreation(), actual.getPrivateSubnetCreation());
        assertFalse(actual.isExistingNetwork());
    }

    private NetworkDto.Builder createNetworkDto() {
        return NetworkDto.builder()
                .withResourceCrn("resource crn")
                .withSubnetMetas(Map.of("subnet-id", new CloudSubnet()))
                .withNetworkCidr("10.0.0.0/16")
                .withCbSubnets(Map.of("cb-subnetId", new CloudSubnet()))
                .withDwxSubnets(Map.of("dvx-subnetId", new CloudSubnet()))
                .withMlxSubnets(Map.of("mlx-subnetId", new CloudSubnet()))
                .withPrivateSubnetCreation(PrivateSubnetCreation.ENABLED)
                .withServiceEndpointCreation(ServiceEndpointCreation.ENABLED)
                .withRegistrationType(RegistrationType.CREATE_NEW);
    }

    private AzureParams createAzureParams() {
        return AzureParams.builder()
                .withNetworkId("azure-network")
                .withNoPublicIp(true)
                .withResourceGroupName("resource-group")
                .withNetworkId("network-id")
                .build();
    }

    private YarnParams createYarnParams() {
        return YarnParams.YarnParamsBuilder.anYarnParams()
                .withQueue("yarn-queue")
                .build();
    }

    private MockParams createMockParams() {
        return MockParams.MockParamsBuilder.aMockParams()
                .withInternetGatewayId("internet-gateway-id")
                .withVpcId("vpc-id")
                .build();
    }

    private AwsParams createAwsParams() {
        return AwsParams.AwsParamsBuilder.anAwsParams()
                .withVpcId("aws-vpc-id")
                .build();
    }

}