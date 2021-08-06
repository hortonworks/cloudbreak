package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_LISTENER_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.elasticloadbalancingv2.model.AmazonElasticLoadBalancingException;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsResourceNameService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerLaunchServiceTest {

    private static final int USER_FACING_PORT = 443;

    private static final int HEALTH_CHECK_PORT = 8443;

    @Mock
    private AwsLoadBalancerCommonService loadBalancerCommonService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private PersistenceRetriever persistenceRetriever;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonElasticLoadBalancingClient loadBalancingClient;

    @Mock
    private AwsResourceNameService resourceNameService;

    @InjectMocks
    private AwsNativeLoadBalancerLaunchService undertTest;

    @BeforeEach
    void setUp() {
        when(awsTaggingService.prepareElasticLoadBalancingTags(any())).thenReturn(List.of(new Tag().withKey("aTag").withValue("aValue")));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenNoLoadBalancerSpecified() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(new ArrayList<>());

        List<CloudResourceStatus> result = undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerAlreadyExistsWithNameAndTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        AmazonServiceException loadBalancerDuplicatedExc = new AmazonServiceException(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE);
        loadBalancerDuplicatedExc.setErrorCode(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(loadBalancerDuplicatedExc);
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        DescribeLoadBalancersResult loadBalancerResult = new DescribeLoadBalancersResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.describeLoadBalancers(any())).thenReturn(loadBalancerResult);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult()
                .withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerListenerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        when(loadBalancingClient.registerListener(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenTargetGroupAlreadyExistsWithNameAndLoadBalancerListenerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        AmazonServiceException amazonServiceException = new AmazonServiceException(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE);
        amazonServiceException.setErrorCode(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(amazonServiceException);
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        DescribeTargetGroupsResult describeTargetGroupsResult = new DescribeTargetGroupsResult()
                .withTargetGroups(targetGroup);
        when(loadBalancingClient.describeTargetGroup(any())).thenReturn(describeTargetGroupsResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        when(loadBalancingClient.registerListener(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerTargetsCouldNotBeRegistered() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(loadBalancingClient, times(1)).registerTargets(any());
        verify(persistenceNotifier, times(3)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(cloudResource.getType())));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenListenerAlreadyExistsAndLoadBalancerTargetsCouldNotBeRegistered() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        AmazonServiceException amazonServiceException = new AmazonServiceException(DUPLICATE_LISTENER_ERROR_CODE);
        amazonServiceException.setErrorCode(DUPLICATE_LISTENER_ERROR_CODE);
        when(loadBalancingClient.registerListener(any())).thenThrow(amazonServiceException);
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        DescribeListenersResult describeListenersResult = new DescribeListenersResult()
                .withListeners(listener);
        when(loadBalancingClient.describeListeners(any())).thenReturn(describeListenersResult);
        when(loadBalancingClient.registerTargets(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(loadBalancingClient, times(1)).registerTargets(any());
        verify(persistenceNotifier, times(3)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(cloudResource.getType())));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenAllResourcesCouldBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient);

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(3)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        verify(loadBalancingClient, times(1)).registerTargets(any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(cloudResource.getType())));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, ResourceStatus.CREATED));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerResourceExistsAlready() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        String aLoadBalancerName = "aLoadBalancerName";
        CloudResource loadBalancerResource = CloudResource.builder()
                .name(aLoadBalancerName)
                .reference("aLoadBalancerArn")
                .type(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.of(loadBalancerResource));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient);

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        verify(loadBalancingClient, times(1)).registerTargets(any());
        verify(loadBalancingClient, times(0)).registerLoadBalancer(any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(cloudResource.getType())));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, ResourceStatus.CREATED));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenTargetGroupResourcesExistAlready() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        CloudResource targetGroupResource = CloudResource.builder()
                .name("aTargetTG443Master")
                .type(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                .reference("aTargetGroupArn")
                .build();
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.of(targetGroupResource));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn("aLoadBalancerTGName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient);

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        verify(loadBalancingClient, times(1)).registerTargets(any());
        verify(loadBalancingClient, times(0)).createTargetGroup(any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_LISTENER.equals(cloudResource.getType())));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, ResourceStatus.CREATED));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerAndTargetGroupAndListenerResourcesExistAlready() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER), any(), any())).thenReturn("aLoadBalancerName");
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String aLoadBalancerTGName = "aLoadBalancerTGName";
        when(resourceNameService.resourceName(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), any(), any(), any())).thenReturn(aLoadBalancerTGName);
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.empty());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        CloudResource listenerResource = CloudResource.builder()
                .name(aLoadBalancerTGName)
                .type(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .reference("aListenerArn")
                .build();
        when(persistenceRetriever.retrieveFirstByTypeAndStatusForStack(eq(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER), eq(CommonStatus.CREATED), any()))
                .thenReturn(Optional.of(listenerResource));
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = undertTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient);

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        verify(loadBalancingClient, times(1)).registerTargets(any());
        verify(loadBalancingClient, times(0)).registerListener(any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, ResourceStatus.CREATED));
        assertTrue(resourceExistsWithTypeAndStatus(statuses, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, ResourceStatus.CREATED));
    }

    private CloudStack getCloudStack() {
        Network network = new Network(new Subnet("10.0.0.0/16"), Map.of(NetworkConstants.VPC_ID, "vpc-avpcarn"));
        return new CloudStack(List.of(), network, null, Map.of(), Map.of(), null, null, null, null, null, null);
    }

    private AwsLoadBalancer getAwsLoadBalancer() {
        AwsLoadBalancer awsLoadBalancer = new AwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
        awsLoadBalancer.getOrCreateListener(USER_FACING_PORT, HEALTH_CHECK_PORT);
        return awsLoadBalancer;
    }

    private boolean resourceExistsWithTypeAndStatus(List<CloudResourceStatus> statuses, ResourceType resourceType, ResourceStatus resourceStatus) {
        return statuses
                .stream()
                .anyMatch(status -> resourceType.equals(status.getCloudResource().getType()) && resourceStatus.equals(status.getStatus()));
    }
}