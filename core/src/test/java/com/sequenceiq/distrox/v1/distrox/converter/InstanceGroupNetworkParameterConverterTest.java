package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.azure.InstanceGroupAzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.gcp.InstanceGroupGcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.mock.InstanceGroupMockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.azure.InstanceGroupAzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.gcp.InstanceGroupGcpNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.mock.InstanceGroupMockNetworkV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class InstanceGroupNetworkParameterConverterTest {

    private static final String ENV_PREFERRED_SUBNET_ID = "preferredSubnetId";

    private static final String STACK_LEVEL_SUBNET_ID = "stackLevelSubnetId";

    private static final List<String> INSTANCE_GROUP_SUBNETS = List.of("instanceGroupSubnet1", "instanceGroupSubnet2");

    private static final Set<String> ENDPOINT_GATEWAY_SUBNET_IDS = Set.of("endpointGatewaySubnetId1", "endpointGatewaySubnetId2");

    private static final Set<String> AZURE_AVAILABILITY_ZONES = Set.of("1", "2", "3");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EnvironmentNetworkResponse environmentNetworkResponse;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private NetworkV4Request networkV4Request;

    @InjectMocks
    private InstanceGroupNetworkParameterConverter underTest;

    @Test
    void convertMockNetworkParametersWhenNoNetworkConfigOnIgOrStackOrEnvLevel() {
        InstanceGroupMockNetworkV1Parameters providerSpecificNetworkParam = null;

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.convert(providerSpecificNetworkParam, environmentNetworkResponse, CloudPlatform.MOCK, networkV4Request));

        assertEquals("Subnet could not be selected for you request, please check your environment and it's network configuration.",
                exception.getMessage());
    }

    @Test
    void convertMockParametersWhenOnlyEnvironmentLevelNetworkIsConfigured() {
        InstanceGroupMockNetworkV1Parameters mockNetworkV1Parameters = new InstanceGroupMockNetworkV1Parameters();
        when(environmentNetworkResponse.getPreferedSubnetId()).thenReturn(ENV_PREFERRED_SUBNET_ID);

        InstanceGroupMockNetworkV4Parameters actual = underTest.convert(mockNetworkV1Parameters, environmentNetworkResponse, CloudPlatform.MOCK,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(ENV_PREFERRED_SUBNET_ID::equals));
    }

    @Test
    void convertMockParametersWhenStackLevelNetworkIsConfigured() {
        InstanceGroupMockNetworkV1Parameters mockNetworkV1Parameters = new InstanceGroupMockNetworkV1Parameters();
        when(networkV4Request.getMock().getSubnetId()).thenReturn(STACK_LEVEL_SUBNET_ID);

        InstanceGroupMockNetworkV4Parameters actual = underTest.convert(mockNetworkV1Parameters, environmentNetworkResponse, CloudPlatform.MOCK,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(STACK_LEVEL_SUBNET_ID::equals));
    }

    @Test
    void convertMockParametersWhenInstanceGroupLevelNetworkIsConfigured() {
        InstanceGroupMockNetworkV1Parameters mockNetworkV1Parameters = new InstanceGroupMockNetworkV1Parameters();
        mockNetworkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);

        InstanceGroupMockNetworkV4Parameters actual = underTest.convert(mockNetworkV1Parameters, environmentNetworkResponse, CloudPlatform.MOCK,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
    }

    @Test
    void convertAwsNetworkParametersWhenNoNetworkConfigOnIgOrStackOrEnvLevel() {
        InstanceGroupAwsNetworkV1Parameters providerSpecificNetworkParam = new InstanceGroupAwsNetworkV1Parameters();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.convert(providerSpecificNetworkParam, environmentNetworkResponse, CloudPlatform.AWS, networkV4Request));

        assertEquals("Subnet could not be selected for you request, please check your environment and it's network configuration.",
                exception.getMessage());
    }

    @Test
    void convertAwsParametersWhenOnlyEnvironmentLevelNetworkIsConfigured() {
        InstanceGroupAwsNetworkV1Parameters networkV1Parameters = null;
        when(environmentNetworkResponse.getPreferedSubnetId()).thenReturn(ENV_PREFERRED_SUBNET_ID);
        when(environmentNetworkResponse.getEndpointGatewaySubnetIds()).thenReturn(null);

        InstanceGroupAwsNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AWS,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(ENV_PREFERRED_SUBNET_ID::equals));
    }

    @Test
    void convertAwsParametersWhenStackLevelNetworkIsConfigured() {
        InstanceGroupAwsNetworkV1Parameters networkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        when(networkV4Request.getAws().getSubnetId()).thenReturn(STACK_LEVEL_SUBNET_ID);
        when(environmentNetworkResponse.getEndpointGatewaySubnetIds()).thenReturn(ENDPOINT_GATEWAY_SUBNET_IDS);

        InstanceGroupAwsNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AWS,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(STACK_LEVEL_SUBNET_ID::equals));
        assertTrue(actual.getEndpointGatewaySubnetIds().containsAll(ENDPOINT_GATEWAY_SUBNET_IDS));
    }

    @Test
    void convertAwsParametersWhenInstanceGroupLevelNetworkIsConfigured() {
        InstanceGroupAwsNetworkV1Parameters networkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);
        when(environmentNetworkResponse.getEndpointGatewaySubnetIds()).thenReturn(null);

        InstanceGroupAwsNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AWS,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
    }

    @Test
    void convertAwsParametersWhenInstanceGroupLevelNetworkAndEnvEndpointGatewayAreConfigured() {
        InstanceGroupAwsNetworkV1Parameters networkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);
        networkV1Parameters.setEndpointGatewaySubnetIds(new ArrayList<>(ENDPOINT_GATEWAY_SUBNET_IDS));

        InstanceGroupAwsNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AWS,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
        assertTrue(actual.getEndpointGatewaySubnetIds().containsAll(ENDPOINT_GATEWAY_SUBNET_IDS));
    }

    @Test
    void convertAzureNetworkParametersWhenNoNetworkConfigOnIgOrStackOrEnvLevel() {
        InstanceGroupAzureNetworkV1Parameters providerSpecificNetworkParam = null;

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.convert(providerSpecificNetworkParam, environmentNetworkResponse, CloudPlatform.AZURE, networkV4Request));

        assertEquals("Subnet could not be selected for you request, please check your environment and it's network configuration.",
                exception.getMessage());
    }

    @Test
    void convertAzureParametersWhenOnlyEnvironmentLevelNetworkIsConfigured() {
        InstanceGroupAzureNetworkV1Parameters networkV1Parameters = new InstanceGroupAzureNetworkV1Parameters();
        when(environmentNetworkResponse.getPreferedSubnetId()).thenReturn(ENV_PREFERRED_SUBNET_ID);

        InstanceGroupAzureNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AZURE,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(ENV_PREFERRED_SUBNET_ID::equals));
        assertTrue(actual.getAvailabilityZones().isEmpty());
    }

    @Test
    void convertAzureParametersWhenStackLevelNetworkIsConfigured() {
        InstanceGroupAzureNetworkV1Parameters networkV1Parameters = new InstanceGroupAzureNetworkV1Parameters();
        networkV1Parameters.setAvailabilityZones(AZURE_AVAILABILITY_ZONES);
        when(networkV4Request.getAzure().getSubnetId()).thenReturn(STACK_LEVEL_SUBNET_ID);

        InstanceGroupAzureNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AZURE,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(STACK_LEVEL_SUBNET_ID::equals));
        assertTrue(actual.getAvailabilityZones().containsAll(AZURE_AVAILABILITY_ZONES));
    }

    @Test
    void convertAzureParametersWhenInstanceGroupLevelNetworkIsConfigured() {
        InstanceGroupAzureNetworkV1Parameters networkV1Parameters = new InstanceGroupAzureNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);

        InstanceGroupAzureNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AZURE,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
        assertTrue(actual.getAvailabilityZones().isEmpty());
    }

    @Test
    void convertAzureParametersWhenInstanceGroupNetworkLevelSubnetAndAvailabilityZonesAreConfigured() {
        InstanceGroupAzureNetworkV1Parameters networkV1Parameters = new InstanceGroupAzureNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);
        networkV1Parameters.setAvailabilityZones(AZURE_AVAILABILITY_ZONES);

        InstanceGroupAzureNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.AZURE,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
        assertTrue(actual.getAvailabilityZones().containsAll(AZURE_AVAILABILITY_ZONES));
    }

    @Test
    void convertGcpNetworkParametersWhenNoNetworkConfigOnIgOrStackOrEnvLevel() {
        InstanceGroupGcpNetworkV1Parameters providerSpecificNetworkParam = new InstanceGroupGcpNetworkV1Parameters();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.convert(providerSpecificNetworkParam, environmentNetworkResponse, CloudPlatform.GCP, networkV4Request));

        assertEquals("Subnet could not be selected for you request, please check your environment and it's network configuration.",
                exception.getMessage());
    }

    @Test
    void convertGcpParametersWhenOnlyEnvironmentLevelNetworkIsConfigured() {
        InstanceGroupGcpNetworkV1Parameters networkV1Parameters = null;
        when(environmentNetworkResponse.getPreferedSubnetId()).thenReturn(ENV_PREFERRED_SUBNET_ID);

        InstanceGroupGcpNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.GCP,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(ENV_PREFERRED_SUBNET_ID::equals));
    }

    @Test
    void convertGcpParametersWhenStackLevelNetworkIsConfigured() {
        InstanceGroupGcpNetworkV1Parameters networkV1Parameters = new InstanceGroupGcpNetworkV1Parameters();
        when(networkV4Request.getGcp().getSubnetId()).thenReturn(STACK_LEVEL_SUBNET_ID);

        InstanceGroupGcpNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.GCP,
                networkV4Request);

        assertTrue(actual.getSubnetIds().stream().allMatch(STACK_LEVEL_SUBNET_ID::equals));
    }

    @Test
    void convertGcpParametersWhenInstanceGroupLevelNetworkIsConfigured() {
        InstanceGroupGcpNetworkV1Parameters networkV1Parameters = new InstanceGroupGcpNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);

        InstanceGroupGcpNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.GCP,
                networkV4Request);

        assertTrue(actual.getSubnetIds().containsAll(INSTANCE_GROUP_SUBNETS));
    }

    static Object [] [] getAvailabilityZones() {
        return new Object [] [] {
                {Set.of()},
                {Set.of("us-west2-a", "us-west2-b")}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAvailabilityZones")
    void convertGcpParametersWithAvailabilityZones(Set<String> availabilityZones) {
        InstanceGroupGcpNetworkV1Parameters networkV1Parameters = new InstanceGroupGcpNetworkV1Parameters();
        networkV1Parameters.setSubnetIds(INSTANCE_GROUP_SUBNETS);
        networkV1Parameters.setAvailabilityZones(availabilityZones);

        InstanceGroupGcpNetworkV4Parameters actual = underTest.convert(networkV1Parameters, environmentNetworkResponse, CloudPlatform.GCP,
                networkV4Request);
        assertEquals(availabilityZones, actual.getAvailabilityZones());
    }
}