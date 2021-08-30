package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@ExtendWith(MockitoExtension.class)
class AwsLoadBalancerCommonServiceTest {

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String CIDR = "10.0.0.0/16";

    private static final int PORT = 443;

    private static final String INSTANCE_NAME = "instance";

    private static final String INSTANCE_ID = "instance-id";

    private static final String SUBNET_ID_1 = "subnetId";

    private static final String SUBNET_ID_2 = "anotherSubnetId";

    private static final String PUBLIC_SUBNET_ID_1 = "publicSubnetId";

    private static final String PUBLIC_SUBNET_ID_2 = "anotherPublicSubnetId";

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @InjectMocks
    private AwsLoadBalancerCommonService underTest;

    @BeforeEach
    public void setup() {
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPrivate() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PRIVATE;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PRIVATE_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicEndpointGatway() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicNoEndpointGateway() {
        AwsNetworkView awsNetworkView = createNetworkView(PUBLIC_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPrivateSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class,
                        () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PRIVATE, awsNetworkView,
                                new CloudLoadBalancer(LoadBalancerType.PRIVATE)));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPublicSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class,
                        () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView,
                                new CloudLoadBalancer(LoadBalancerType.PUBLIC)));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsOnlyEndpointGatewaySet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsWhenEndpointGatewaySetAndInstanceGroupNetworkContainsSubnetForMultiAz() {
        AwsNetworkView awsNetworkView = createNetworkView(null, PUBLIC_ID_1);
        LoadBalancerType loadBalancerType = LoadBalancerType.PUBLIC;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancerWithEndpointGateay(loadBalancerType, List.of(SUBNET_ID_1, SUBNET_ID_2),
                List.of(PUBLIC_SUBNET_ID_1, PUBLIC_SUBNET_ID_2));

        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);

        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_SUBNET_ID_1, PUBLIC_SUBNET_ID_2), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsWhenSubnetsArePrivateAndInstanceGroupNetworkContainsSubnetForMultiAz() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        LoadBalancerType loadBalancerType = LoadBalancerType.PRIVATE;
        CloudLoadBalancer cloudLoadBalancer = createCloudLoadBalancer(loadBalancerType, List.of(SUBNET_ID_1, SUBNET_ID_2));

        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(loadBalancerType, awsNetworkView, cloudLoadBalancer);

        assertEquals(Set.of(PRIVATE_ID_1, SUBNET_ID_1, SUBNET_ID_2), subnetIds);
    }

    @Test
    public void testConvertLoadBalancerNewPrivate() {
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PRIVATE, PRIVATE_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNAL, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PRIVATE_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerExistingPrivate() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(existingLoadBalancer), LoadBalancerType.PRIVATE, PRIVATE_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(existingLoadBalancer, awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNAL, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PRIVATE_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerNewPublic() {
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PUBLIC, PUBLIC_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNET_FACING, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PUBLIC_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    @Test
    public void testConvertLoadBalancerExistingPublic() {
        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(existingLoadBalancer), LoadBalancerType.PUBLIC, PUBLIC_ID_1);

        assertNotNull(awsLoadBalancer);
        assertEquals(existingLoadBalancer, awsLoadBalancer);
        assertEquals(AwsLoadBalancerScheme.INTERNET_FACING, awsLoadBalancer.getScheme());
        assertEquals(Set.of(PUBLIC_ID_1), awsLoadBalancer.getSubnetIds());
        assertEquals(1, awsLoadBalancer.getListeners().size());
        AwsListener listener = awsLoadBalancer.getListeners().get(0);
        assertEquals(PORT, listener.getPort());
        assertNotNull(listener.getTargetGroup());
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(PORT, targetGroup.getPort());
        assertEquals(Set.of(INSTANCE_ID), targetGroup.getInstanceIds());
    }

    private AwsLoadBalancer setupAndRunConvertLoadBalancer(List<AwsLoadBalancer> existingLoadBalancers, LoadBalancerType type, String subnetId) {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(subnetId, null);

        Map<String, List<String>> instanceIdsByGroupName = instances.stream()
                .collect(Collectors.groupingBy(CloudResource::getGroup, mapping(CloudResource::getInstanceId, toList())));

        return underTest.convertLoadBalancer(createCloudLoadBalancer(type), instanceIdsByGroupName, awsNetworkView, existingLoadBalancers);
    }

    private List<CloudResource> createInstances() {
        return List.of(CloudResource.builder()
                .name(INSTANCE_NAME)
                .instanceId(INSTANCE_ID)
                .type(AWS_INSTANCE)
                .status(CREATED)
                .params(Map.of())
                .group(INSTANCE_NAME)
                .build());
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerType type) {
        return createCloudLoadBalancer(type, List.of());
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerType type, List<String> instanceGroupNetworkSubnetIds) {
        Group group = new Group(INSTANCE_NAME, GATEWAY, List.of(), null, null, null, null,
                null, null, 100, null, createGroupNetwork(instanceGroupNetworkSubnetIds));
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, PORT), Set.of(group));
        return cloudLoadBalancer;
    }

    private CloudLoadBalancer createCloudLoadBalancerWithEndpointGateay(LoadBalancerType type, List<String> instanceGroupNetworkSubnetIds,
            List<String> endpointGatewaySubnetIds) {
        Group group = new Group(INSTANCE_NAME, GATEWAY, List.of(), null, null, null, null,
                null, null, 100, null, createGroupNetwork(instanceGroupNetworkSubnetIds, endpointGatewaySubnetIds));
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, PORT), Set.of(group));
        return cloudLoadBalancer;
    }

    private GroupNetwork createGroupNetwork(List<String> instanceGroupNetworkSubnetIds) {
        Set<GroupSubnet> groupSubnets = instanceGroupNetworkSubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, groupSubnets, new HashMap<>());
    }

    private GroupNetwork createGroupNetwork(List<String> instanceGroupNetworkSubnetIds, List<String> endpointGatewaySubnetIds) {
        Set<GroupSubnet> groupSubnets = instanceGroupNetworkSubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        Set<GroupSubnet> endpointGatewaySubnets = endpointGatewaySubnetIds.stream()
                .map(GroupSubnet::new)
                .collect(Collectors.toSet());
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, groupSubnets, endpointGatewaySubnets, new HashMap<>());
    }

    private AwsNetworkView createNetworkView(String subnetId, String endpointGatewaSubnetId) {
        return new AwsNetworkView(createNetwork(subnetId, endpointGatewaSubnetId));
    }

    private Network createNetwork(String subnetId, String endpointGatewaSubnetId) {
        Map<String, Object> params = new HashMap<>();
        params.put(SUBNET_ID, subnetId);
        params.put(VPC_ID, VPC_ID);
        if (StringUtils.isNotEmpty(endpointGatewaSubnetId)) {
            params.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaSubnetId);
        }
        return new Network(new Subnet(CIDR), params);
    }
}