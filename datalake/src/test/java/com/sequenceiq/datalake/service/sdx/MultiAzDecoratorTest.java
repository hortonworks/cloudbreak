package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

class MultiAzDecoratorTest {

    private static final String PREFERRED_SUBNET_ID = "aPreferredSubnetId";

    private static final String PREFERRED_SUBNET_ID2 = "anotherPreferredSubnetId";

    private MultiAzDecorator underTest;

    @BeforeEach
    void setUp() {
        underTest = new MultiAzDecorator();
    }

    @Test
    void decorateStackRequestWithMultiAzShouldUseTheSingleEnvironmentPreferredSubnetIdWhenClusterShapeIsNotHA() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.LIGHT_DUTY);

        Assertions.assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().stream()
                        .allMatch(PREFERRED_SUBNET_ID::equals)));
    }

    @Test
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsWhenClusterShapeIsHA() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Set<String> subnetIds = Set.of(PREFERRED_SUBNET_ID, PREFERRED_SUBNET_ID2);
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        network.setPreferedSubnetIds(subnetIds);
        network.setSubnetIds(subnetIds);
        network.setSubnetMetas(Map.of(
                PREFERRED_SUBNET_ID, new CloudSubnet("id1", PREFERRED_SUBNET_ID, "eu-central-1a", "10.0.0.0/24", false, true, true, SubnetType.PUBLIC),
                PREFERRED_SUBNET_ID2, new CloudSubnet("id1", PREFERRED_SUBNET_ID2, "eu-central-1b", "10.0.1.0/24", false, true, true, SubnetType.PUBLIC)
        ));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setTunnel(Tunnel.DIRECT);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.MEDIUM_DUTY_HA);

        Assertions.assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
    }

    @Test
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsButOneSubnetPerAzForGatewayGroupWhenClusterShapeIsHA() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Set<String> subnetIds = Set.of(PREFERRED_SUBNET_ID, PREFERRED_SUBNET_ID2);
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        network.setPreferedSubnetIds(subnetIds);
        network.setSubnetIds(subnetIds);
        network.setSubnetMetas(Map.of(
                PREFERRED_SUBNET_ID, new CloudSubnet("id1", PREFERRED_SUBNET_ID, "eu-central-1a", "10.0.0.0/24", false, true, true, SubnetType.PUBLIC),
                PREFERRED_SUBNET_ID2, new CloudSubnet("id1", PREFERRED_SUBNET_ID2, "eu-central-1a", "10.0.1.0/24", false, true, true, SubnetType.PUBLIC)
        ));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setTunnel(Tunnel.DIRECT);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.MEDIUM_DUTY_HA);

        Assertions.assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> InstanceGroupType.GATEWAY.equals(
                        ig.getType()) ? ig.getNetwork().getAws().getSubnetIds().size() == 1 : ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(InstanceGroupType instanceGroupType) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setType(instanceGroupType);
        return instanceGroup;
    }
}