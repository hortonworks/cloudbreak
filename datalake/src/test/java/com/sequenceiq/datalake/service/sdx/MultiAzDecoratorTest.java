package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.DeploymentRestriction.DATALAKE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.network.aws.InstanceGroupAwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
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

    private static final String SUBNET_ID2 = "secondSubnetId";

    private static final String SUBNET_ID3 = "thirdSubnetId";

    private static final String SUBNET_ID4 = "fourthSubnetId";

    private static final String PLATFORM_AWS = AWS.name();

    private static final String PLATFORM_AZURE = CloudPlatform.AZURE.name();

    private static final String PLATFORM_GCP = CloudPlatform.GCP.name();

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

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzShouldUseTheSingleEnvironmentPreferredSubnetIdWhenClusterShapeIsNotHAAndAws(SdxClusterShape clusterShape) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));

        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setCloudPlatform(PLATFORM_AWS);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, clusterShape);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().stream()
                        .allMatch(PREFERRED_SUBNET_ID::equals)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzShouldNotModifyRequestWhenClusterShapeIsHAAndAwsAndRequestContainsIGLevelNetwork(SdxClusterShape sdxClusterShape) {
        Set<String> subnetIds = Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2);
        InstanceGroupNetworkV4Request instanceGroupNetworkV4Request = new InstanceGroupNetworkV4Request();
        InstanceGroupAwsNetworkV4Parameters instanceGroupAwsNetworkV4Parameters = instanceGroupNetworkV4Request.createAws();
        instanceGroupAwsNetworkV4Parameters.setSubnetIds(new ArrayList<>(subnetIds));
        StackV4Request stackV4Request = new StackV4Request();
        InstanceGroupV4Request gatewayInstanceGroup = getInstanceGroupV4Request(InstanceGroupType.GATEWAY);
        gatewayInstanceGroup.setNetwork(instanceGroupNetworkV4Request);
        InstanceGroupV4Request idBrokerInstanceGroup = getInstanceGroupV4Request(InstanceGroupType.CORE);
        idBrokerInstanceGroup.setNetwork(instanceGroupNetworkV4Request);
        stackV4Request.setInstanceGroups(List.of(gatewayInstanceGroup, idBrokerInstanceGroup));

        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setTunnel(Tunnel.DIRECT);
        environment.setCloudPlatform(PLATFORM_AWS);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, sdxClusterShape);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    static Stream<Arguments> testDecorateStackWithMultiWhenSubnetShouldBeGotFromEnvProvider() {
        return Stream.of(
                arguments(SdxClusterShape.MEDIUM_DUTY_HA, Tunnel.DIRECT, Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2)),
                arguments(SdxClusterShape.ENTERPRISE, Tunnel.DIRECT, Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2)),
                arguments(SdxClusterShape.MEDIUM_DUTY_HA, Tunnel.CLUSTER_PROXY, Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2)),
                arguments(SdxClusterShape.ENTERPRISE, Tunnel.CLUSTER_PROXY, Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2)),
                arguments(SdxClusterShape.MEDIUM_DUTY_HA, Tunnel.CCM, Set.of(SUBNET_ID3, SUBNET_ID4)),
                arguments(SdxClusterShape.ENTERPRISE, Tunnel.CCM, Set.of(SUBNET_ID3, SUBNET_ID4)),
                arguments(SdxClusterShape.MEDIUM_DUTY_HA, Tunnel.CCMV2, Set.of(SUBNET_ID3, SUBNET_ID4)),
                arguments(SdxClusterShape.ENTERPRISE, Tunnel.CCMV2, Set.of(SUBNET_ID3, SUBNET_ID4)),
                arguments(SdxClusterShape.MEDIUM_DUTY_HA, Tunnel.CCMV2_JUMPGATE, Set.of(SUBNET_ID3, SUBNET_ID4)),
                arguments(SdxClusterShape.ENTERPRISE, Tunnel.CCMV2_JUMPGATE, Set.of(SUBNET_ID3, SUBNET_ID4))
        );
    }

    @ParameterizedTest
    @MethodSource("testDecorateStackWithMultiWhenSubnetShouldBeGotFromEnvProvider")
    void testDecorateStackRequestWithMultiAzShouldGetSubnetsFromEnvResponseWhenClusterShapeIsHAOnAws(
            SdxClusterShape sdxClusterShape,
            Tunnel tunnel,
            Set<String> expectedSubnetIds) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Map<String, CloudSubnet> cloudSubnetMap = Map.of(
                PREFERRED_SUBNET_ID,
                new CloudSubnet.Builder()
                        .id("id1")
                        .name(PREFERRED_SUBNET_ID)
                        .availabilityZone("eu-central-1a")
                        .cidr("10.0.0.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .build(),
                SUBNET_ID2,
                new CloudSubnet.Builder()
                        .id("id2")
                        .name(SUBNET_ID2)
                        .availabilityZone("eu-central-1b")
                        .cidr("10.0.1.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .build(),
                SUBNET_ID3,
                new CloudSubnet.Builder()
                        .id("id3")
                        .name(SUBNET_ID3)
                        .availabilityZone("eu-central-1c")
                        .cidr("10.0.2.0/24")
                        .privateSubnet(true)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(true)
                        .type(SubnetType.PRIVATE)
                        .build(),
                SUBNET_ID4,
                new CloudSubnet.Builder()
                        .id("id4")
                        .name(SUBNET_ID4)
                        .availabilityZone("eu-central-1d")
                        .cidr("10.0.3.0/24")
                        .privateSubnet(true)
                        .mapPublicIpOnLaunch(false)
                        .igwAvailable(true)
                        .type(SubnetType.PRIVATE)
                        .build()
        );
        Set<String> subnetIds = cloudSubnetMap.keySet();
        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(subnetIds, cloudSubnetMap, cloudSubnetMap, tunnel);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, sdxClusterShape);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(expectedSubnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsWhenClusterShapeIsHAAndAwsAndEnvironmentCbSubnetsCouldBeUsedByDeploymentRestriction(
            SdxClusterShape sdxClusterShape) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Set<String> subnetIds = Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2);
        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(subnetIds, null, Map.of(
                PREFERRED_SUBNET_ID,
                new CloudSubnet.Builder()
                        .id("id1")
                        .name(PREFERRED_SUBNET_ID)
                        .availabilityZone("eu-central-1d")
                        .cidr("10.0.0.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .deploymentRestrictions(Set.of(DATALAKE))
                        .build(),
                SUBNET_ID2,
                new CloudSubnet.Builder()
                        .id("id1")
                        .name(SUBNET_ID2)
                        .availabilityZone("eu-central-1b")
                        .cidr("10.0.1.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .deploymentRestrictions(Set.of(DATALAKE))
                        .build()
        ), Tunnel.DIRECT);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, sdxClusterShape);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzShouldPickMultipleSubnetsButOneSubnetPerAzForGatewayGroupWhenClusterShapeIsHAAndAws(SdxClusterShape sdxClusterShape) {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Set<String> subnetIds = Set.of(PREFERRED_SUBNET_ID, SUBNET_ID2);
        Map<String, CloudSubnet> subnetMetas = Map.of(
                PREFERRED_SUBNET_ID,
                new CloudSubnet.Builder()
                        .id("id1")
                        .name(PREFERRED_SUBNET_ID)
                        .availabilityZone("eu-central-1a")
                        .cidr("10.0.0.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .build(),
                SUBNET_ID2,
                new CloudSubnet.Builder()
                        .id("id1")
                        .name(SUBNET_ID2)
                        .availabilityZone("eu-central-1a")
                        .cidr("10.0.1.0/24")
                        .privateSubnet(false)
                        .mapPublicIpOnLaunch(true)
                        .igwAvailable(true)
                        .type(SubnetType.PUBLIC)
                        .build()
        );
        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(subnetIds, subnetMetas, subnetMetas, Tunnel.DIRECT);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, sdxClusterShape);

        assertTrue(stackV4Request.getInstanceGroups().stream()
                .allMatch(ig -> InstanceGroupType.GATEWAY.equals(
                        ig.getType()) ? ig.getNetwork().getAws().getSubnetIds().size() == 1 : ig.getNetwork().getAws().getSubnetIds().containsAll(subnetIds)));
        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CloudPlatform.class, mode = Mode.EXCLUDE, names = {"AWS", "AZURE", "GCP"})
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
                String.format("Multi Az on Azure is not unsupported for Cluster Shape '%s'", clusterShape));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, mode = Mode.EXCLUDE, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzTestWhenGcpAndBadClusterShape(SdxClusterShape clusterShape) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(PLATFORM_GCP);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.decorateStackRequestWithMultiAz(new StackV4Request(), environment, clusterShape));

        assertThat(illegalStateException).hasMessage(
                String.format("Multi Az on GCP is not unsupported for Cluster Shape '%s'",
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

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SdxClusterShape.class, names = {"MEDIUM_DUTY_HA", "ENTERPRISE"})
    void decorateStackRequestWithMultiAzTestWhenGcpAndSuccess(SdxClusterShape clusterShape) {
        StackV4Request stackV4Request = new StackV4Request();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(PLATFORM_GCP);

        underTest.decorateStackRequestWithMultiAz(stackV4Request, environment, clusterShape);

        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @Test
    void decorateStackRequestWithPreviousNetworkAWS() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Map<String, Set<String>> subnetsByAz = Map.of("az1", Set.of("subnet1", "subnet2"), "az2", Set.of("subnet3", "subnet4"));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(String.valueOf(AWS));

        underTest.decorateStackRequestWithPreviousNetwork(stackV4Request, environment, subnetsByAz);

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .filter(ig -> InstanceGroupType.GATEWAY.equals(ig.getType()))
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().size() == 2));

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .filter(ig -> !InstanceGroupType.GATEWAY.equals(ig.getType()))
                .allMatch(ig -> ig.getNetwork().getAws().getSubnetIds().containsAll(List.of("subnet1", "subnet2", "subnet3", "subnet4"))));

        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @Test
    void decorateStackRequestWithPreviousNetworkAzure() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Map<String, Set<String>> subnetsByAz = Map.of("1", Set.of("subnet1", "subnet2"), "2", Set.of("subnet1", "subnet2"),
                "3", Set.of("subnet1", "subnet2"));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(String.valueOf(AZURE));

        underTest.decorateStackRequestWithPreviousNetwork(stackV4Request, environment, subnetsByAz);

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .allMatch(ig -> ig.getNetwork().getAzure().getSubnetIds().containsAll(List.of("subnet1", "subnet2"))));

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .allMatch(ig -> ig.getNetwork().getAzure().getAvailabilityZones().containsAll(Set.of("1", "2", "3"))));

        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    @Test
    void decorateStackRequestWithPreviousNetworkGcp() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setInstanceGroups(List.of(getInstanceGroupV4Request(InstanceGroupType.GATEWAY), getInstanceGroupV4Request(InstanceGroupType.CORE)));
        Map<String, Set<String>> subnetsByAz = Map.of("1", Set.of("subnet1", "subnet2"), "2", Set.of("subnet1", "subnet2"),
                "3", Set.of("subnet1", "subnet2"));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setCloudPlatform(String.valueOf(GCP));

        underTest.decorateStackRequestWithPreviousNetwork(stackV4Request, environment, subnetsByAz);

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .allMatch(ig -> ig.getNetwork().getGcp().getSubnetIds().containsAll(List.of("subnet1", "subnet2"))));

        assertTrue(stackV4Request
                .getInstanceGroups()
                .stream()
                .allMatch(ig -> ig.getNetwork().getGcp().getAvailabilityZones().containsAll(Set.of("1", "2", "3"))));

        assertThat(stackV4Request.isEnableMultiAz()).isTrue();
    }

    private InstanceGroupV4Request getInstanceGroupV4Request(InstanceGroupType instanceGroupType) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setType(instanceGroupType);
        return instanceGroup;
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(Set<String> subnetIds, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> cbSubnets, Tunnel tunnel) {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setPreferedSubnetId(PREFERRED_SUBNET_ID);
        network.setPreferedSubnetIds(subnetIds);
        network.setSubnetIds(subnetIds);
        network.setSubnetMetas(subnetMetas);
        network.setCbSubnets(cbSubnets);
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setTunnel(tunnel);
        environment.setCloudPlatform(PLATFORM_AWS);
        return environment;
    }
}