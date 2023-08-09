package com.sequenceiq.datalake.service.sdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

class MultiAzDecoratorTest {

    private static final String PREFERRED_SUBNET_ID = "aPreferredSubnetId";

    private static final String PREFERRED_SUBNET_ID2 = "anotherPreferredSubnetId";

    private static final String PLATFORM_AWS = CloudPlatform.AWS.name();

    private static final String PLATFORM_AZURE = CloudPlatform.AZURE.name();

    private MultiAzDecorator underTest;

    @BeforeEach
    void setUp() {
        underTest = new MultiAzDecorator();
    }

    @Test
    void decorateStackRequestWithAwsNativeTest() {
        StackV4Request stackV4Request = new StackV4Request();

        underTest.decorateStackRequestWithAwsNative(stackV4Request);

        assertThat(stackV4Request.getVariant()).isEqualTo("AWS_NATIVE");
    }

    @Test
    void decorateStackRequestWithMultiAzShouldUseTheSingleEnvironmentPreferredSubnetIdWhenClusterShapeIsNotHAAndAws() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setCloudPlatform(PLATFORM_AWS);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.LIGHT_DUTY);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().stream()
                        .allMatch(PREFERRED_SUBNET_ID::equals)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @Test
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsWhenClusterShapeIsHAAndAws() {
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
        environment.setCloudPlatform(PLATFORM_AWS);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.MEDIUM_DUTY_HA);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @Test
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsButOneSubnetPerAzForGatewayGroupWhenClusterShapeIsHAAndAws() {
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
        environment.setCloudPlatform(PLATFORM_AWS);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, SdxClusterShape.MEDIUM_DUTY_HA);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> InstanceGroupType.GATEWAY.equals(
                        ig.getType()) ? ig.getNetwork().getAws().getSubnetIds().size() == 1 : ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, mode = Mode.EXCLUDE, names = {"AWS", "AZURE"})
    void decorateStackRequestWithMultiAzTestWhenUnsupportedCloudPlatform(CloudPlatform cloudPlatform) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(cloudPlatform.name());

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.decorateStackRequestWithMultiAz(new StackV4Request(), environment, SdxClusterShape.LIGHT_DUTY));

        assertThat(illegalStateException).hasMessage("Encountered enableMultiAz==true for unsupported cloud platform " + cloudPlatform);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzTestWhenAzureAndBadClusterShape(SdxClusterShape clusterShape) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(PLATFORM_AZURE);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.decorateStackRequestWithMultiAz(new StackV4Request(), environment, clusterShape));

        assertThat(illegalStateException).hasMessage(
                String.format("Encountered clusterShape=%s with isMultiAzEnabledByDefault()==false. Azure multi AZ is unsupported for such shapes.",
                        clusterShape));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzTestWhenAzureAndSuccess(SdxClusterShape clusterShape) {
        StackV4Request stackV4Request = new StackV4Request();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(PLATFORM_AZURE);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, clusterShape);

        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(InstanceGroupType instanceGroupType) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setType(instanceGroupType);
        return instanceGroup;
    }
}