package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.CommonStatus.FAILED;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsTargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;

import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;

@ExtendWith(MockitoExtension.class)
class AwsLoadBalancerLaunchServiceTest {

    private static final String PRIVATE_ID_1 = "private-id-1";

    private static final String PUBLIC_ID_1 = "public-id-1";

    private static final String CIDR = "10.0.0.0/16";

    private static final int PORT = 443;

    private static final String INSTANCE_NAME = "instance";

    private static final String STACK_NAME = "stack";

    private static final String TARGET_GROUP_ARN = "arn://targetgroup";

    private static final String LOAD_BALANCER_ARN = "arn://loadbalancer";

    private static final int TG_INDEX = 0;

    private static final int LB_INDEX = 1;

    private static final int LIS_INDEX = 2;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @Mock
    private CloudFormationWaiter waiters;

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
    private AwsModelService awsModelService;

    @Mock
    private AwsLoadBalancerCommonService awsLoadBalancerCommonService;

    @Mock
    private AwsPageCollector awsPageCollector;

    @InjectMocks
    private AwsLoadBalancerLaunchService underTest;

    @Test
    void testUpdateCloudformationWithPrivateLoadBalancer() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, Set.of(LoadBalancerType.PRIVATE));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    private ListStackResourcesResponse createListStackResourcesResponse(List<StackResourceSummary> resourceSummaries) {
        return ListStackResourcesResponse.builder().stackResourceSummaries(resourceSummaries).build();
    }

    @Test
    void testUpdateCloudformationWithPublicLoadBalancerNoEndpointGateway() {
        Network network = createNetwork(PUBLIC_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PUBLIC);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, Set.of(LoadBalancerType.PUBLIC));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        assertEquals(LoadBalancerTypeAttribute.PUBLIC,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testUpdateCloudformationWithEndpointGatewayAndPrivateSubnet() {
        Network network = createNetwork(PRIVATE_ID_1, PUBLIC_ID_1);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        verify(cfClient, times(2)).updateStack(any());
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = statuses.stream()
                .map(CloudResourceStatus::getCloudResource)
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactlyInAnyOrder(LoadBalancerTypeAttribute.PUBLIC, LoadBalancerTypeAttribute.PRIVATE);
    }

    @Test
    void testUpdateCloudformationWithLoadBalancerMissingTargetGroup() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: target group %s not found.",
                AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        firstUpdateSummaries.remove(TG_INDEX);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class, () ->
                        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testUpdateCloudformationWithLoadBalancerMissingTargetGroupArn() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: target group %s arn not found.",
                AwsTargetGroup.getTargetGroupName(PORT, AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        StackResourceSummary tgSummary = firstUpdateSummaries.get(TG_INDEX).toBuilder().physicalResourceId(null).build();
        firstUpdateSummaries.remove(TG_INDEX);
        firstUpdateSummaries.add(tgSummary);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class, () ->
                        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testUpdateCloudformationWithLoadBalancerMissingLoadBalancer() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s not found.",
                AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        firstUpdateSummaries.remove(LB_INDEX);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);
        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class, () ->
                        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testUpdateCloudformationWithLoadBalancerMissingLoadBalancerArn() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        String expectedError = String.format("Could not create load balancer listeners: load balancer %s arn not found.",
                AwsLoadBalancer.getLoadBalancerName(AwsLoadBalancerScheme.INTERNAL));
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        StackResourceSummary lbSummary = firstUpdateSummaries.get(LB_INDEX).toBuilder().physicalResourceId(null).build();
        firstUpdateSummaries.remove(LB_INDEX);
        firstUpdateSummaries.add(lbSummary);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);

        CloudConnectorException exception =
                assertThrows(CloudConnectorException.class, () ->
                        underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null));

        verify(cfClient, times(1)).updateStack(any());
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testSetLoadBalancerMetadata() {
        AwsLoadBalancer loadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        loadBalancer.getOrCreateListener(PORT, ProtocolEnum.TCP, new HealthProbeParameters("/", PORT, NetworkProtocol.HTTPS, 10, 2));

        underTest.setLoadBalancerMetadata(List.of(loadBalancer), createListStackResourcesResponse(createFullSummaries(Set.of(LoadBalancerType.PRIVATE))));

        assertEquals(LOAD_BALANCER_ARN, loadBalancer.getArn());
        AwsListener listener = loadBalancer.getListeners().iterator().next();
        assert listener.areTargetGroupArnsSet();
        AwsTargetGroup targetGroup = listener.getTargetGroup();
        assertEquals(TARGET_GROUP_ARN, targetGroup.getArn());
    }

    @Test
    void testUpdateCloudformationSuccess() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED, statuses.get(0).getStatus());
        assertEquals(CREATED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testUpdateCloudformationLoadBalancerCreateFailure() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        StackResourceSummary stackResourceSummary = secondUpdateSummaries.get(LB_INDEX).toBuilder().resourceStatus(ResourceStatus.CREATE_FAILED).build();
        secondUpdateSummaries.remove(LB_INDEX);
        secondUpdateSummaries.add(stackResourceSummary);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, types);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testUpdateCloudformationListenerCreateFailure() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        StackResourceSummary stackResourceSummary = secondUpdateSummaries.get(LIS_INDEX).toBuilder().resourceStatus(ResourceStatus.CREATE_FAILED).build();
        secondUpdateSummaries.remove(LIS_INDEX);
        secondUpdateSummaries.add(stackResourceSummary);
        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, Set.of(LoadBalancerType.PRIVATE));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testUpdateCloudformationTargetGroupCreateFailure() {
        Network network = createNetwork(PRIVATE_ID_1, null);
        Set<LoadBalancerType> types = Set.of(LoadBalancerType.PRIVATE);
        List<StackResourceSummary> firstUpdateSummaries = createFirstUpdateSummaries(types);
        List<StackResourceSummary> secondUpdateSummaries = createFullSummaries(types);
        StackResourceSummary stackResourceSummary = secondUpdateSummaries.get(TG_INDEX).toBuilder().resourceStatus(ResourceStatus.CREATE_FAILED).build();
        secondUpdateSummaries.remove(TG_INDEX);
        secondUpdateSummaries.add(stackResourceSummary);

        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(Collections.emptyList()))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(firstUpdateSummaries))
                .thenReturn(createListStackResourcesResponse(secondUpdateSummaries));
        setupMocksForUpdate(network, Set.of(LoadBalancerType.PRIVATE));
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone("az1"));

        List<CloudResourceStatus> statuses = underTest.updateCloudformationWithLoadBalancers(ac, cloudStack, null, null);

        assertEquals(1, statuses.size());
        assertEquals(com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED, statuses.get(0).getStatus());
        assertEquals(FAILED, statuses.get(0).getCloudResource().getStatus());
        assertEquals(ELASTIC_LOAD_BALANCER, statuses.get(0).getCloudResource().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                statuses.get(0).getCloudResource().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testCheckForLoadBalancerAndTargetGroupResourcesExistingResources() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();

        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE))));

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assert result;
    }

    @Test
    void testCheckForLoadBalancerAndTargetGroupResourcesMissingLoadBalancer() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        StackResourceSummary stackResourceSummary = summaries.get(LB_INDEX).toBuilder().logicalResourceId(null).build();
        summaries.remove(LB_INDEX);
        summaries.add(stackResourceSummary);

        when(cfClient.listStackResources(any())).thenReturn(createListStackResourcesResponse(summaries));

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    @Test
    void testCheckForLoadBalancerAndTargetGroupResourcesMissingTargetGrup() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFirstUpdateSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        StackResourceSummary stackResourceSummary = summaries.get(TG_INDEX).toBuilder().logicalResourceId(null).build();
        summaries.remove(TG_INDEX);
        summaries.add(stackResourceSummary);

        when(cfClient.listStackResources(any())).thenReturn(createListStackResourcesResponse(summaries));

        boolean result = underTest.checkForLoadBalancerAndTargetGroupResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    @Test
    void testCheckForListenerResourcesExistingResources() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();

        when(cfClient.listStackResources(any()))
                .thenReturn(createListStackResourcesResponse(createFullSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE))));

        boolean result = underTest.checkForListenerResources(cfClient, STACK_NAME, loadBalancers);

        assert result;
    }

    @Test
    void testCheckForListenerResourcesMissingListener() {
        List<AwsLoadBalancer> loadBalancers = setupAwsLoadBalancers();
        List<StackResourceSummary> summaries = createFullSummaries(Set.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE));
        StackResourceSummary stackResourceSummary = summaries.get(LIS_INDEX).toBuilder().logicalResourceId(null).build();
        summaries.remove(LIS_INDEX);
        summaries.add(stackResourceSummary);

        when(cfClient.listStackResources(any())).thenReturn(createListStackResourcesResponse(summaries));

        boolean result = underTest.checkForListenerResources(cfClient, STACK_NAME, loadBalancers);

        assertFalse(result);
    }

    private Network createNetwork(String subnetId, String endpointGatewaySubnetId) {
        Map<String, Object> params = new HashMap<>();
        params.put(SUBNET_ID, subnetId);
        params.put(VPC_ID, VPC_ID);
        if (StringUtils.isNotEmpty(endpointGatewaySubnetId)) {
            params.put(ENDPOINT_GATEWAY_SUBNET_ID, endpointGatewaySubnetId);
        }
        return new Network(new Subnet(CIDR), params);
    }

    private CloudLoadBalancer createCloudLoadBalancer(LoadBalancerType type) {
        Group group = Group.builder().build();
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
                StackResourceSummary tgSummary = StackResourceSummary.builder()
                        .logicalResourceId(AwsTargetGroup.getTargetGroupName(PORT, scheme))
                        .physicalResourceId(TARGET_GROUP_ARN)
                        .resourceStatus(ResourceStatus.CREATE_COMPLETE)
                        .build();
                summaries.add(tgSummary);
                StackResourceSummary lbSummary = StackResourceSummary.builder()
                        .logicalResourceId(AwsLoadBalancer.getLoadBalancerName(scheme))
                        .physicalResourceId(LOAD_BALANCER_ARN)
                        .resourceStatus(ResourceStatus.CREATE_COMPLETE)
                        .build();
                summaries.add(lbSummary);
            }
            if (creatListeners) {
                StackResourceSummary lSummary = StackResourceSummary.builder()
                        .logicalResourceId(AwsListener.getListenerName(PORT, scheme))
                        .resourceStatus(ResourceStatus.CREATE_COMPLETE)
                        .build();
                summaries.add(lSummary);
            }
        }
        return summaries;
    }

    private void setupMocksForUpdate(Network network, Set<LoadBalancerType> types) {
        List<CloudLoadBalancer> loadBalancers = new ArrayList<>();
        List<AwsLoadBalancer> awsLoadBalancers = new ArrayList<>();
        for (LoadBalancerType type : types) {
            loadBalancers.add(createCloudLoadBalancer(type));
            AwsLoadBalancerScheme scheme = AwsLoadBalancerScheme.INTERNET_FACING;
            if (LoadBalancerType.PRIVATE.equals(type)) {
                scheme = AwsLoadBalancerScheme.INTERNAL;
            }
            AwsLoadBalancer awsLoadBalancer = new AwsLoadBalancer(scheme);
            awsLoadBalancer.getOrCreateListener(PORT, ProtocolEnum.TCP, new HealthProbeParameters("/", PORT, NetworkProtocol.HTTPS, 10, 2));
            awsLoadBalancers.add(awsLoadBalancer);
        }
        when(cfStackUtil.getCfStackName(any())).thenReturn(STACK_NAME);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(cfClient);
        when(cfClient.updateStack(any())).thenReturn(null);
        when(cloudFormationTemplateBuilder.build(any(ModelContext.class))).thenReturn("{}");
        when(awsStackRequestHelper.createUpdateStackRequest(any(), any(), anyString(), anyString())).thenReturn(null);
        when(cfClient.waiters()).thenReturn(waiters);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getId()).thenReturn(1L);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.value()).thenReturn("region");
        when(cloudStack.getLoadBalancers()).thenReturn(loadBalancers);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(awsModelService.buildDefaultModelContext(any(), any(), any())).thenReturn(new ModelContext());
        when(awsLoadBalancerCommonService.getAwsLoadBalancers(eq(loadBalancers), any(), any())).thenReturn(awsLoadBalancers);
        lenient().when(awsPageCollector.getAllRouteTables(any(), any())).thenReturn(List.of());
    }

    private List<AwsLoadBalancer> setupAwsLoadBalancers() {
        AwsLoadBalancer privateLb = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        privateLb.getOrCreateListener(PORT, ProtocolEnum.TCP, new HealthProbeParameters("/", PORT, NetworkProtocol.HTTPS, 10, 2));
        AwsLoadBalancer publicLb = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNET_FACING);
        publicLb.getOrCreateListener(PORT, ProtocolEnum.TCP, new HealthProbeParameters("/", PORT, NetworkProtocol.HTTPS, 10, 2));
        return List.of(privateLb, publicLb);
    }
}
