package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.CommonStatus.FAILED;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.converter.LoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;

@PrepareForTest(AwsPageCollector.class)
@RunWith(PowerMockRunner.class)
public class AwsLaunchServiceLoadBalancerTest {

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String CIDR = "10.0.0.0/16";

    private static final int PORT = 443;

    private static final String INSTANCE_NAME = "instance";

    private static final String INSTANCE_ID = "instance-id";

    private static final String STACK_NAME = "stack";

    private static final String TARGET_GROUP_ARN = "arn://targetgroup";

    private static final String LOAD_BALANCER_ARN = "arn://loadbalancer";

    private static final String REGION = "region";

    private static final int TG_INDEX = 0;

    private static final int LB_INDEX = 1;

    private static final int LIS_INDEX = 2;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsClient awsClient;

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

    @Mock
    private AwsModelService awsModelService;

    @Mock
    private LoadBalancerTypeConverter loadBalancerTypeConverter;

    @InjectMocks
    private AwsLoadBalancerLaunchService underTest;

    @Before
    public void setup() {
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PRIVATE))).thenReturn(AwsLoadBalancerScheme.INTERNAL);
        when(loadBalancerTypeConverter.convert(eq(LoadBalancerType.PUBLIC))).thenReturn(AwsLoadBalancerScheme.INTERNET_FACING);
    }

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
        PowerMockito.mockStatic(AwsPageCollector.class);
        PowerMockito.when(AwsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());

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
        PowerMockito.mockStatic(AwsPageCollector.class);
        PowerMockito.when(AwsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());

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
        PowerMockito.mockStatic(AwsPageCollector.class);
        PowerMockito.when(AwsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());

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
    public void testUpdateCloudformationWithPrivateLoadBalancer() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE));
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        verify(result, times(4)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithPublicLoadBalancerNoEndpointGateway() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PUBLIC_ID_1, null);
        Network network = createNetwork(PUBLIC_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PUBLIC);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PUBLIC));
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        verify(result, times(4)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithEndpointGatewayAndPrivateSubnet() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, PUBLIC_ID_1);
        Network network = createNetwork(PRIVATE_ID_1, PUBLIC_ID_1);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        verify(result, times(5)).getStackResourceSummaries();
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingTargetGroup() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: target group %s not found.",
            AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        firstUpdateSummaries.remove(TG_INDEX);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () ->
                underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingTargetGroupArn() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: target group %s arn not found.",
            AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        StackResourceSummary tgSummary = firstUpdateSummaries.get(TG_INDEX);
        tgSummary.setPhysicalResourceId(null);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () ->
                underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingLoadBalancer() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s not found.",
            AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        firstUpdateSummaries.remove(LB_INDEX);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () ->
                underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testUpdateCloudformationWithLoadBalancerMissingLoadBalancerArn() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s arn not found.",
            AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        StackResourceSummary lbSummary = firstUpdateSummaries.get(LB_INDEX);
        lbSummary.setPhysicalResourceId(null);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        CloudConnectorException exception =
            assertThrows(CloudConnectorException.class, () ->
                underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        verify(result, times(2)).getStackResourceSummaries();
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    public void testSetLoadBalancerMetadata() {
        AwsLoadBalancer loadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        loadBalancer.getOrCreateListener(PORT, PORT);

        when(result.getStackResourceSummaries()).thenReturn(createFullSummaries(Set.of(LoadBalancerType.PRIVATE)));

        underTest.setLoadBalancerMetadata(List.of(loadBalancer), result);

        assertEquals(LOAD_BALANCER_ARN, loadBalancer.getArn());
        AwsListener listener = loadBalancer.getListeners().iterator().next();
        assert listener.areTargetGroupArnsSet();
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(TARGET_GROUP_ARN, targetGroup.getArn());
    }

    @Test
    public void testUpdateCloudformationSuccess() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED, statuses.get(0).getStatus());
        assertEquals(CREATED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
    }

    @Test
    public void testUpdateCloudformationLoadBalancerCreateFailure() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        secondUpdateSummaries.get(LB_INDEX).setResourceStatus(ResourceStatus.CREATE_FAILED);

        setupMocksForUpdate(awsNetworkView, network, instances, types);
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
    }

    @Test
    public void testUpdateCloudformationListenerCreateFailure() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        secondUpdateSummaries.get(LIS_INDEX).setResourceStatus(ResourceStatus.CREATE_FAILED);

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE));
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
    }

    @Test
    public void testUpdateCloudformationTargetGroupCreateFailure() {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(PRIVATE_ID_1, null);
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        secondUpdateSummaries.get(TG_INDEX).setResourceStatus(ResourceStatus.CREATE_FAILED);

        setupMocksForUpdate(awsNetworkView, network, instances, Set.of(LoadBalancerType.PRIVATE));
        when(result.getStackResourceSummaries())
            .thenReturn(List.of())
            .thenReturn(firstUpdateSummaries)
            .thenReturn(firstUpdateSummaries)
            .thenReturn(secondUpdateSummaries);

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
    }

    @Test
    public void testCheckForLoadBalancerAndTargetGroupResourcesExistingResources() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();

        when(result.getStackResourceSummaries()).thenReturn(createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE)));
        when(cfClient.listStackResources(any())).thenReturn(result);

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assert result;
    }

    @Test
    public void testCheckForLoadBalancerAndTargetGroupResourcesMissingLoadBalancer() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        summaries.get(LB_INDEX).setLogicalResourceId(null);

        when(result.getStackResourceSummaries()).thenReturn(summaries);
        when(cfClient.listStackResources(any())).thenReturn(result);

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    @Test
    public void testCheckForLoadBalancerAndTargetGroupResourcesMissingTargetGrup() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        summaries.get(TG_INDEX).setLogicalResourceId(null);

        when(result.getStackResourceSummaries()).thenReturn(summaries);
        when(cfClient.listStackResources(any())).thenReturn(result);

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    @Test
    public void testCheckForListenerResourcesExistingResources() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();

        when(result.getStackResourceSummaries()).thenReturn(createFullSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE)));
        when(cfClient.listStackResources(any())).thenReturn(result);

        boolean result = underTest.checkForListenerResources(cfClient, STACK_NAME, loadBalancers);

        assert result;
    }

    @Test
    public void testCheckForListenerResourcesMissingListener() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFullSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        summaries.get(LIS_INDEX).setLogicalResourceId(null);

        when(result.getStackResourceSummaries()).thenReturn(summaries);
        when(cfClient.listStackResources(any())).thenReturn(result);

        boolean result = underTest.checkForListenerResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    private AwsLoadBalancer setupAndRunConvertLoadBalancer(List<AwsLoadBalancer> existingLoadBalancers, LoadBalancerType type, String subnetId) {
        List<CloudResource> instances = createInstances();
        AwsNetworkView awsNetworkView = createNetworkView(subnetId, null);

        PowerMockito.mockStatic(AwsPageCollector.class);
        PowerMockito.when(AwsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());

        return underTest.convertLoadBalancer(createCloudLoadBalancer(type), instances, awsNetworkView, existingLoadBalancers);
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

    private List<StackResourceSummary> createFirstUpdateSummaries(Set<LoadBalancerType> types) {
        return createSummaries(types, true, false);
    }

    private List<StackResourceSummary> createFullSummaries(Set<LoadBalancerType> types) {
        return createSummaries(types, true, true);
    }

    private List<StackResourceSummary> createSummaries(Set<LoadBalancerType> types, boolean createLbAndTg, boolean creatListeners) {
        List<StackResourceSummary> summaries = new ArrayList<>();
        for (LoadBalancerType type : types) {
            AwsLoadBalancerScheme scheme = LoadBalancerType.PRIVATE.equals(type) ?
                AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
            if (createLbAndTg) {
                StackResourceSummary tgSummary = new StackResourceSummary();
                tgSummary.setLogicalResourceId(AwsTargetGroup.getTargetGroupName(PORT, scheme));
                tgSummary.setPhysicalResourceId(TARGET_GROUP_ARN);
                tgSummary.setResourceStatus(ResourceStatus.CREATE_COMPLETE);
                summaries.add(tgSummary);
                StackResourceSummary lbSummary = new StackResourceSummary();
                lbSummary.setLogicalResourceId(AwsLoadBalancer.getLoadBalancerName(scheme));
                lbSummary.setPhysicalResourceId(LOAD_BALANCER_ARN);
                lbSummary.setResourceStatus(ResourceStatus.CREATE_COMPLETE);
                summaries.add(lbSummary);
            }
            if (creatListeners) {
                StackResourceSummary lSummary = new StackResourceSummary();
                lSummary.setLogicalResourceId(AwsListener.getListenerName(PORT, scheme));
                lSummary.setResourceStatus(ResourceStatus.CREATE_COMPLETE);
                summaries.add(lSummary);
            }
        }
        return summaries;
    }

    private void setupMocksForUpdate(AwsNetworkView awsNetworkView, Network network, List<CloudResource> instances, Set<LoadBalancerType> types) {
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        for (LoadBalancerType type : types) {
            loadBalancers.add(createCloudLoadBalancer(type));

        }
        when(cfStackUtil.getCfStackName(any())).thenReturn(STACK_NAME);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(cfClient);
        when(cfClient.updateStack(any())).thenReturn(null);
        when(cfClient.listStackResources(any())).thenReturn(result);
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
        when(cloudStack.getNetwork()).thenReturn(network);
        when(awsModelService.buildDefaultModelContext(any(), any(), any())).thenReturn(new ModelContext());
        PowerMockito.mockStatic(AwsPageCollector.class);
        PowerMockito.when(AwsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());
    }

    private List<AwsLoadBalancer> setupAwsLoadBalancers() {
        AwsLoadBalancer privateLb = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        privateLb.getOrCreateListener(PORT, PORT);
        AwsLoadBalancer publicLb = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        publicLb.getOrCreateListener(PORT, PORT);
        return List.of(privateLb, publicLb);
    }
}
