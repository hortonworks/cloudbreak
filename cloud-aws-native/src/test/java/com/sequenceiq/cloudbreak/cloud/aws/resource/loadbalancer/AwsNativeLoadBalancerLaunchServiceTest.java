package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_LISTENER_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer.AwsNativeLoadBalancerLaunchService.DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerLaunchServiceTest {

    private static final int USER_FACING_PORT = 443;

    private static final int HEALTH_CHECK_PORT = 8443;

    private static final String STACK_NAME = "stackName";

    private static final String STACK_CRN = "stackCrn";

    private static final Long STACK_ID = 123L;

    private static final String LB_NAME_INTERNAL = "stackn-LBInternal-20221028001020";

    private static final String LB_NAME_INTERNAL_NEW = "stackn-LBInternal-20221028102030";

    private static final String LB_NAME_INTERNAL_NO_HASH = "stackn-LBInternal";

    private static final String TG_NAME_INTERNAL = "sta-TG443Internal-20221028001020";

    private static final String TG_NAME_INTERNAL_NEW = "sta-TG443Internal-20221028102030";

    private static final String TG_NAME_INTERNAL_NO_HASH = "sta-TG443Internal";

    private static final String LB_NAME_EXTERNAL = "stackn-LBExternal-20221028001020";

    private static final String LB_NAME_EXTERNAL_NEW = "stackn-LBExternal-20221028102030";

    private static final String LB_NAME_EXTERNAL_NO_HASH = "stackn-LBExternal";

    private static final String TG_NAME_EXTERNAL = "sta-TG443External-20221028001020";

    private static final String TG_NAME_EXTERNAL_NEW = "sta-TG443External-20221028102030";

    private static final String TG_NAME_EXTERNAL_NO_HASH = "sta-TG443External";

    private static final String INTERNAL = "Internal";

    private static final String EXTERNAL = "External";

    @Mock
    private AwsLoadBalancerCommonService loadBalancerCommonService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private PersistenceRetriever persistenceRetriever;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonElasticLoadBalancingClient loadBalancingClient;

    @Mock
    private AwsResourceNameService resourceNameService;

    @InjectMocks
    private AwsNativeLoadBalancerLaunchService underTest;

    @BeforeEach
    void setUp() {
        when(awsTaggingService.prepareElasticLoadBalancingTags(any())).thenReturn(List.of(new Tag().withKey("aTag").withValue("aValue")));
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        when(cloudContext.getCrn()).thenReturn(STACK_CRN);
        when(cloudContext.getName()).thenReturn(STACK_NAME);
        when(cloudContext.getId()).thenReturn(STACK_ID);
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenNoNeedToRegisterTargetGroup() {
        CloudStack stack = mock(CloudStack.class);
        AwsLoadBalancer loadBalancer = mock(AwsLoadBalancer.class);
        when(loadBalancer.getScheme()).thenReturn(AwsLoadBalancerScheme.INTERNAL);

        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        CloudResource loadBalancerResource = CloudResource.builder()
                .withName(LB_NAME_INTERNAL)
                .withReference("aLoadBalancerArn")
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of(loadBalancerResource));

        when(stack.getGroups()).thenReturn(emptyList());
        when(stack.getLoadBalancers()).thenReturn(emptyList());
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(loadBalancer));

        underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, false);

        verify(loadBalancer, never()).getListeners();
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenNoLoadBalancerSpecified() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(new ArrayList<>());

        List<CloudResourceStatus> result = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true);

        assertTrue(result.isEmpty());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerAlreadyExistsWithNameAndTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        AmazonServiceException loadBalancerDuplicatedExc = new AmazonServiceException(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE);
        loadBalancerDuplicatedExc.setErrorCode(DUPLICATE_LOAD_BALANCER_NAME_ERROR_CODE);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(loadBalancerDuplicatedExc);
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        DescribeLoadBalancersResult loadBalancerResult = new DescribeLoadBalancersResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.describeLoadBalancers(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult()
                .withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerListenerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerListener(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

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
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        AmazonServiceException amazonServiceException = new AmazonServiceException(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE);
        amazonServiceException.setErrorCode(DUPLICATE_TARGET_GROUP_NAME_ERROR_CODE);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(amazonServiceException);
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        DescribeTargetGroupsResult describeTargetGroupsResult = new DescribeTargetGroupsResult()
                .withTargetGroups(targetGroup);
        when(loadBalancingClient.describeTargetGroup(any())).thenReturn(describeTargetGroupsResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerListener(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

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
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenThrow(new AmazonElasticLoadBalancingException("something went wrong"));

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

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
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, INTERNAL)).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(LB_NAME_INTERNAL_NEW)).thenReturn(LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, INTERNAL, USER_FACING_PORT))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceNameService.trimHash(TG_NAME_INTERNAL_NEW)).thenReturn(TG_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
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
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenAllResourcesCouldBeCreated(boolean internal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, scheme)).thenReturn(lbNameNew);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(internal ? LB_NAME_INTERNAL_NO_HASH : LB_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, scheme, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(internal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenLoadBalancerResourceExistsAlreadyWithSameSchemeAndTargetGroupAndListener(boolean internal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, scheme)).thenReturn(lbNameNew);
        String lbName = internal ? LB_NAME_INTERNAL : LB_NAME_EXTERNAL;
        String lbNameNoHash = internal ? LB_NAME_INTERNAL_NO_HASH : LB_NAME_EXTERNAL_NO_HASH;
        when(resourceNameService.trimHash(lbName)).thenReturn(lbNameNoHash);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(lbNameNoHash);
        CloudResource loadBalancerResource = CloudResource.builder()
                .withName(lbName)
                .withReference("aLoadBalancerArn")
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of(loadBalancerResource));
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, scheme, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(internal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenLoadBalancerResourceExistsAlreadyButDifferentSchemeAndTargetGroupAndListener(boolean existingInternal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, schemeNew)).thenReturn(lbNameNew);
        String lbName = existingInternal ? LB_NAME_INTERNAL : LB_NAME_EXTERNAL;
        when(resourceNameService.trimHash(lbName)).thenReturn(existingInternal ? LB_NAME_INTERNAL_NO_HASH : LB_NAME_EXTERNAL_NO_HASH);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(existingInternal ? LB_NAME_EXTERNAL_NO_HASH : LB_NAME_INTERNAL_NO_HASH);
        CloudResource loadBalancerResource = CloudResource.builder()
                .withName(lbName)
                .withReference("aLoadBalancerArn")
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of(loadBalancerResource));
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, schemeNew, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(existingInternal ? TG_NAME_EXTERNAL_NO_HASH : TG_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenTargetGroupResourceExistsAlreadyWithSameSchemeAndLoadBalancerAndListener(boolean internal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, scheme)).thenReturn(lbNameNew);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(internal ? LB_NAME_INTERNAL_NO_HASH : LB_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgName = internal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource targetGroupResource = CloudResource.builder()
                .withName(tgName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                .withReference("aTargetGroupArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID))
                .thenReturn(List.of(targetGroupResource));
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, scheme, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        String tgNameNoHash = internal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH;
        when(resourceNameService.trimHash(tgName)).thenReturn(tgNameNoHash);
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(tgNameNoHash);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenTargetGroupResourceExistsAlreadyButDifferentSchemeAndLoadBalancerAndListener(boolean existingInternal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, schemeNew)).thenReturn(lbNameNew);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(existingInternal ? LB_NAME_EXTERNAL_NO_HASH : LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgName = existingInternal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource targetGroupResource = CloudResource.builder()
                .withName(tgName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP)
                .withReference("aTargetGroupArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID))
                .thenReturn(List.of(targetGroupResource));
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, schemeNew, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        when(resourceNameService.trimHash(tgName)).thenReturn(existingInternal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH);
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(existingInternal ? TG_NAME_EXTERNAL_NO_HASH : TG_NAME_INTERNAL_NO_HASH);
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenListenerResourceExistsAlreadyWithSameSchemeAndLoadBalancerAndTargetGroup(boolean internal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, scheme)).thenReturn(lbNameNew);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(internal ? LB_NAME_INTERNAL_NO_HASH : LB_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, scheme, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        String tgNameNoHash = internal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH;
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(tgNameNoHash);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        String listenerName = internal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceNameService.trimHash(listenerName)).thenReturn(tgNameNoHash);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenListenerResourceExistsAlreadyButDifferentSchemeAndLoadBalancerAndTargetGroup(boolean existingInternal) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER, STACK_NAME, schemeNew)).thenReturn(lbNameNew);
        when(resourceNameService.trimHash(lbNameNew)).thenReturn(existingInternal ? LB_NAME_EXTERNAL_NO_HASH : LB_NAME_INTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = new LoadBalancer()
                .withLoadBalancerArn("anARN");
        CreateLoadBalancerResult loadBalancerResult = new CreateLoadBalancerResult().withLoadBalancers(loadBalancer);
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.resourceName(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_NAME, schemeNew, USER_FACING_PORT))
                .thenReturn(tgNameNew);
        String tgNameNoHash = existingInternal ? TG_NAME_EXTERNAL_NO_HASH : TG_NAME_INTERNAL_NO_HASH;
        when(resourceNameService.trimHash(tgNameNew)).thenReturn(tgNameNoHash);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = new TargetGroup()
                .withTargetGroupArn("aTargetGroupArn");
        CreateTargetGroupResult createTargetGroupResult = new CreateTargetGroupResult()
                .withTargetGroups(List.of(targetGroup));
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        String listenerName = existingInternal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceNameService.trimHash(listenerName)).thenReturn(existingInternal ? TG_NAME_INTERNAL_NO_HASH : TG_NAME_EXTERNAL_NO_HASH);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        Listener listener = new Listener()
                .withListenerArn("aListenerArn");
        CreateListenerResult createListenerResult = new CreateListenerResult()
                .withListeners(listener);
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(new RegisterTargetsResult());

        List<CloudResourceStatus> statuses = underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient,
                true);

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

    private CloudStack getCloudStack() {
        Network network = new Network(new Subnet("10.0.0.0/16"), Map.of(NetworkConstants.VPC_ID, "vpc-avpcarn"));
        return new CloudStack(List.of(), network, null, Map.of(), Map.of(), null, null, null, null, null, null);
    }

    private AwsLoadBalancer getAwsLoadBalancer() {
        return getAwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
    }

    private AwsLoadBalancer getAwsLoadBalancer(AwsLoadBalancerScheme scheme) {
        AwsLoadBalancer awsLoadBalancer = new AwsLoadBalancer(scheme);
        awsLoadBalancer.getOrCreateListener(USER_FACING_PORT, HEALTH_CHECK_PORT);
        return awsLoadBalancer;
    }

    private boolean resourceExistsWithTypeAndStatus(List<CloudResourceStatus> statuses, ResourceType resourceType, ResourceStatus resourceStatus) {
        return statuses
                .stream()
                .anyMatch(status -> resourceType.equals(status.getCloudResource().getType()) && resourceStatus.equals(status.getStatus()));
    }

}
