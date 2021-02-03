package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;

@RunWith(MockitoJUnitRunner.class)
public class AwsLaunchServiceLoadBalancerTest {

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String SUBNET_ID = "subnetId";

    private static final String VPC_ID = "vpcId";

    private static final String ENDPOINT_GATEWAY_SUBNET_ID = "endpointGatewaySubnetId";

    private static final String CIDR = "10.0.0.0/16";

    private static final int PORT = 443;

    private static final String INSTANCE_NAME = "instance";

    private static final String INSTANCE_ID = "instance-id";

    private static final String STACK_NAME = "stack";

    private static final String TARGET_GROUP_ARN = "arn://targetgroup";

    private static final String LOAD_BALANCER_ARN = "arn://loadbalancer";

    private static final String REGION = "region";

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonCloudFormationRetryClient cfRetryClient;

    @Mock
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @Mock
    private Waiter<DescribeStacksRequest> updateWaiter;

    @Mock
    private AmazonCloudFormationWaiters waiters;

    @Mock
    private AmazonCloudFormationClient cfClient;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private ListStackResourcesResult result;

    @InjectMocks
    private AwsLaunchService underTest;

    @Test
    public void testSelectLoadBalancerSubnetIdsPrivate() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PRIVATE, awsNetworkView);
        assertEquals(Set.of(PRIVATE_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicEndpointGatway() {
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsPublicNoEndpointGateway() {
        AwsNetworkView awsNetworkView = createNetworkView(PUBLIC_ID_1, null);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPrivateSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PRIVATE, awsNetworkView));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsMissingPublicSubnet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, null);
        String expectedError = "Unable to configure load balancer: Could not identify subnets.";
        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView));
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSelectLoadBalancerSubnetIdsOnlyEndpointGatwaySet() {
        AwsNetworkView awsNetworkView = createNetworkView(null, PUBLIC_ID_1);
        Set<String> subnetIds = underTest.selectLoadBalancerSubnetIds(LoadBalancerType.PUBLIC, awsNetworkView);
        assertEquals(Set.of(PUBLIC_ID_1), subnetIds);
    }

    @Test
    public void testConvertLoadBalancerNewPrivate() {
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        // Returning false - private subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(false);

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
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        // Returning false - private subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(false);

        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
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
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        // Returning true - public subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(true);

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
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        // Returning true - public subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(true);

        AwsLoadBalancer existingLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
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

    @Test
    public void testConvertLoadBalancerMismatchedTypes() {
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        // Returning false - private subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(false);

        AwsLoadBalancer awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PUBLIC, PUBLIC_ID_1);

        assertNull(awsLoadBalancer);

        // Returning true - public subnet
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), anyString(), anyString())).thenReturn(true);

        awsLoadBalancer = setupAndRunConvertLoadBalancer(List.of(), LoadBalancerType.PRIVATE, PRIVATE_ID_1);

        assertNull(awsLoadBalancer);
    }

    @Test
    public void testUpdateCloudformationWithPrivateLoadBalancer() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = new Network(new Subnet(CIDR));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE), false);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
            REGION, amazonEC2Client, network, awsNetworkView, false);

        verify(cfRetryClient, times(2)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(2)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithPublicLoadBalancerNoEndpointGateway() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PUBLIC_ID_1, null);
        Network network = new Network(new Subnet(CIDR));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PUBLIC_ID_1), anyString())).thenReturn(true);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PUBLIC), false);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
            REGION, amazonEC2Client, network, awsNetworkView, false);

        verify(cfRetryClient, times(2)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(2)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithEndpointGatewayAndPrivateSubnet() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        Network network = new Network(new Subnet(CIDR));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PUBLIC_ID_1), anyString())).thenReturn(true);
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), false);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
            REGION, amazonEC2Client, network, awsNetworkView, false);

        verify(cfRetryClient, times(2)).updateStack(any());
        verify(amazonEC2Client, times(2)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(2)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(4)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithMismatchedLoadBalancers() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        Network network = new Network(new Subnet(CIDR));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PUBLIC_ID_1), anyString())).thenReturn(false);
        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(true);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), true);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
            REGION, amazonEC2Client, network, awsNetworkView, false));

        verify(cfRetryClient, times(0)).updateStack(any());
        verify(amazonEC2Client, times(2)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(2)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(0)).getStackResourceSummaries();
        assert exception.getMessage().startsWith("Can not create all requested AWS load balancers.");
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingTargetGroup() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = new Network(new Subnet(CIDR));
        String expectedError = String.format("Could not create load balancer listeners: target group %s not found.",
            AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE), false);
        List<StackResourceSummary> summaries = createSummaries(Set.of(LoadBalancerType.PRIVATE), false);
        summaries.remove(0);
        when(result.getStackResourceSummaries()).thenReturn(summaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
                REGION, amazonEC2Client, network, awsNetworkView, false));

        verify(cfRetryClient, times(1)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(1)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingTargetGroupArn() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = new Network(new Subnet(CIDR));
        String expectedError = String.format("Could not create load balancer listeners: target group %s arn not found.",
            AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE), false);
        List<StackResourceSummary> summaries = createSummaries(Set.of(LoadBalancerType.PRIVATE), false);
        StackResourceSummary tgSummary = summaries.get(0);
        tgSummary.setPhysicalResourceId(null);
        when(result.getStackResourceSummaries()).thenReturn(summaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
                REGION, amazonEC2Client, network, awsNetworkView, false));

        verify(cfRetryClient, times(1)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(1)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingLoadBalancer() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = new Network(new Subnet(CIDR));
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s not found.",
            AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE), false);
        List<StackResourceSummary> summaries = createSummaries(Set.of(LoadBalancerType.PRIVATE), false);
        summaries.remove(1);
        when(result.getStackResourceSummaries()).thenReturn(summaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
                REGION, amazonEC2Client, network, awsNetworkView, false));

        verify(cfRetryClient, times(1)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingLoadBalancerArn() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = new Network(new Subnet(CIDR));
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s arn not found.",
            AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));

        when(awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(any(), eq(PRIVATE_ID_1), anyString())).thenReturn(false);
        when(amazonEC2Client.describeRouteTables(any())).thenReturn(new DescribeRouteTablesResult());
        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE), false);
        List<StackResourceSummary> summaries = createSummaries(Set.of(LoadBalancerType.PRIVATE), false);
        StackResourceSummary lbSummary = summaries.get(1);
        lbSummary.setPhysicalResourceId(null);
        when(result.getStackResourceSummaries()).thenReturn(summaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () -> underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null, instances,
                REGION, amazonEC2Client, network, awsNetworkView, false));

        verify(cfRetryClient, times(1)).updateStack(any());
        verify(amazonEC2Client, times(1)).describeRouteTables(any());
        verify(awsSubnetIgwExplorer, times(1)).hasInternetGatewayOfSubnet(any(), anyString(), anyString());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSetLoadBalancerMetadata() {
        AwsLoadBalancer loadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        loadBalancer.getOrCreateListener(PORT, PORT);
        when(result.getStackResourceSummaries()).thenReturn(createSummaries(Set.of(LoadBalancerType.PRIVATE), false));

        underTest.setLoadBalancerMetadata(List.of(loadBalancer), result);

        assertEquals(LOAD_BALANCER_ARN, loadBalancer.getArn());
        AwsListener listener = loadBalancer.getListeners().iterator().next();
        assert listener.areTargetGroupArnsSet();
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(TARGET_GROUP_ARN, targetGroup.getArn());
    }

    private AwsLoadBalancer setupAndRunConvertLoadBalancer(List<AwsLoadBalancer> existingLoadBalancers, LoadBalancerType type, String subnetId) {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(subnetId, null);

        return underTest.convertLoadBalancer(createCloudLoadBalancer(type), instances, awsNetworkView, amazonEC2Client, existingLoadBalancers);
    }

    private AwsNetworkView createNetworkView(String subnetId, String endpointGatewaSubnetId) {
        Map<String, Object> params = new HashMap<>();
        params.put(SUBNET_ID, subnetId);
        params.put(VPC_ID, VPC_ID);
        if (StringUtils.isNotEmpty(endpointGatewaSubnetId)) {
            params.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaSubnetId);
        }
        String cidr = CIDR;
        Network network = new Network(new Subnet(cidr), params);
        return new AwsNetworkView(network);
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
        Group group = new Group(INSTANCE_NAME, GATEWAY, List.of(), null, null, null, null, null, null, 100, null);
        CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(type);
        cloudLoadBalancer.addPortToTargetGroupMapping(new TargetGroupPortPair(PORT, PORT), Set.of(group));
        return cloudLoadBalancer;
    }

    private List<StackResourceSummary> createSummaries(Set<LoadBalancerType> types, boolean invertMatch) {
        List<StackResourceSummary> summaries = new ArrayList<>();
        for (LoadBalancerType type : types) {
            AwsLoadBalancerScheme scheme;
            if (!invertMatch) {
                scheme = LoadBalancerType.PRIVATE.equals(type) ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
            } else {
                scheme = LoadBalancerType.PUBLIC.equals(type) ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
            }
            StackResourceSummary tgSummary = new StackResourceSummary();
            tgSummary.setLogicalResourceId(AwsTargetGroup.getTargetGroupName(PORT, scheme));
            tgSummary.setPhysicalResourceId(TARGET_GROUP_ARN);
            summaries.add(tgSummary);
            StackResourceSummary lbSummary = new StackResourceSummary();
            lbSummary.setLogicalResourceId(AwsLoadBalancer.getLoadBalancerName(scheme));
            lbSummary.setPhysicalResourceId(LOAD_BALANCER_ARN);
            summaries.add(lbSummary);
        }
        return summaries;
    }

    private void setupMocksForUpdate(AwsNetworkView awsNetworkView, Network network, List<CloudResource> instances,
            Set<LoadBalancerType> types, boolean invertMatch) {
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        for (LoadBalancerType type : types) {
            loadBalancers.add(createCloudLoadBalancer(type));

        }
        when(result.getStackResourceSummaries()).thenReturn(createSummaries(types, invertMatch));

        when(awsNetworkService.getExistingSubnetCidr(any(), any())).thenReturn(List.of(CIDR));
        when(awsNetworkService.getVpcCidrs(any(), any())).thenReturn(List.of(CIDR));
        when(cfStackUtil.getCfStackName(any())).thenReturn(STACK_NAME);
        when(awsClient.createCloudFormationRetryClient(any(), anyString())).thenReturn(cfRetryClient);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(cfClient);
        when(cfRetryClient.updateStack(any())).thenReturn(null);
        when(cfRetryClient.listStackResources(any())).thenReturn(result);
        when(cloudFormationTemplateBuilder.build(any(ModelContext.class))).thenReturn("{}");
        when(awsStackRequestHelper.createUpdateStackRequest(any(), any(), anyString(), anyString())).thenReturn(null);
        when(cfClient.waiters()).thenReturn(waiters);
        when(waiters.stackUpdateComplete()).thenReturn(updateWaiter);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(1L);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn("region");
        when(cloudStack.getLoadBalancers()).thenReturn(loadBalancers);
    }
}
