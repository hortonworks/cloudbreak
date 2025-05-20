package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.DUPLICATE_LISTENER;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.DUPLICATE_LOAD_BALANCER_NAME;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.DUPLICATE_TARGET_GROUP_NAME;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeListenersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ElasticLoadBalancingV2Exception;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.LoadBalancer;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;

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

    private static final String LB_NAME_GWAYPRIV_NEW = "stackn-LBGwayPriv-20221028102030";

    private static final String LB_NAME_GWAYPRIV_NO_HASH = "stackn-LBGwayPriv";

    private static final String TG_NAME_GWAYPRIV_NEW = "sta-TG443GwayPriv-20221028102030";

    private static final String TG_NAME_GWAYPRIV_NO_HASH = "sta-TG443GwayPriv";

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

    @Mock
    private AwsLoadBalancerCommonService awsLoadBalancerCommonService;

    @Mock
    private AwsNativeLoadBalancerSecurityGroupProvider awsNativeLoadBalancerSecurityGroupProvider;

    @InjectMocks
    private AwsNativeLoadBalancerLaunchService underTest;

    @BeforeEach
    void setUp() {
        when(awsTaggingService.prepareElasticLoadBalancingTags(any())).thenReturn(List.of(Tag.builder().key("aTag").value("aValue").build()));
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
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerAlreadyExistsWithNameAndTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        AwsServiceException loadBalancerDuplicatedExc = AwsServiceException.builder()
                .message(DUPLICATE_LOAD_BALANCER_NAME)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(DUPLICATE_LOAD_BALANCER_NAME).build())
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerLoadBalancer(any())).thenThrow(loadBalancerDuplicatedExc);
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        DescribeLoadBalancersResponse loadBalancerResponse = DescribeLoadBalancersResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.describeLoadBalancers(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                cloudResourceArgumentCaptor.getValue().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerTargetGroupCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(1)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertEquals(ResourceType.ELASTIC_LOAD_BALANCER, cloudResourceArgumentCaptor.getValue().getType());
        assertEquals(LoadBalancerTypeAttribute.PRIVATE,
                cloudResourceArgumentCaptor.getValue().getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class));
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerListenerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerListener(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(LoadBalancerTypeAttribute.PRIVATE);
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenTargetGroupAlreadyExistsWithNameAndLoadBalancerListenerCouldNotBeCreated() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message(DUPLICATE_TARGET_GROUP_NAME)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(DUPLICATE_TARGET_GROUP_NAME).build())
                .build();
        when(loadBalancingClient.createTargetGroup(any())).thenThrow(amazonServiceException);
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        DescribeTargetGroupsResponse describeTargetGroupsResponse = DescribeTargetGroupsResponse.builder().targetGroups(targetGroup).build();
        when(loadBalancingClient.describeTargetGroup(any())).thenReturn(describeTargetGroupsResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        when(loadBalancingClient.registerListener(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

        Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.launchLoadBalancerResources(authenticatedContext, stack, persistenceNotifier, loadBalancingClient, true));

        ArgumentCaptor<CloudResource> cloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceArgumentCaptor.capture(), any());
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER.equals(cloudResource.getType())));
        assertTrue(cloudResourceArgumentCaptor.getAllValues().stream()
                .anyMatch(cloudResource -> ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP.equals(cloudResource.getType())));
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(LoadBalancerTypeAttribute.PRIVATE);
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenLoadBalancerTargetsCouldNotBeRegistered() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder().listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResponse = CreateListenerResponse.builder().listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResponse);
        when(loadBalancingClient.registerTargets(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(LoadBalancerTypeAttribute.PRIVATE);
    }

    @Test
    void testLaunchLoadBalancerResourcesWhenListenerAlreadyExistsAndLoadBalancerTargetsCouldNotBeRegistered() {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer()));
        when(resourceNameService.loadBalancer(STACK_NAME, INTERNAL, authenticatedContext.getCloudContext())).thenReturn(LB_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, INTERNAL, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(TG_NAME_INTERNAL_NEW);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .message(DUPLICATE_LISTENER)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(DUPLICATE_LISTENER).build())
                .build();
        when(loadBalancingClient.registerListener(any())).thenThrow(amazonServiceException);
        Listener listener = Listener.builder().listenerArn("aListenerArn").build();
        DescribeListenersResponse describeListenersResponse = DescribeListenersResponse.builder().listeners(listener).build();
        when(loadBalancingClient.describeListeners(any())).thenReturn(describeListenersResponse);
        when(loadBalancingClient.registerTargets(any())).thenThrow(ElasticLoadBalancingV2Exception.builder().message("something went wrong").build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(LoadBalancerTypeAttribute.PRIVATE);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AwsLoadBalancerScheme.class)
    void testLaunchLoadBalancerResourcesWhenAllResourcesCouldBeCreated(AwsLoadBalancerScheme awsLoadBalancerScheme) {
        CloudStack stack = getCloudStack();
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(getAwsLoadBalancer(awsLoadBalancerScheme)));
        String lbNameNew = getLbName(awsLoadBalancerScheme);
        String loadBalancerSchemeName = awsLoadBalancerScheme.resourceName();
        when(resourceNameService.loadBalancer(STACK_NAME, loadBalancerSchemeName, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn(("anARN")).build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
        String tgNameNew = getTgName(awsLoadBalancerScheme);
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, loadBalancerSchemeName, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder().listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResponse = CreateListenerResponse.builder().listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResponse);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(Enum.valueOf(LoadBalancerTypeAttribute.class, awsLoadBalancerScheme.getLoadBalancerType().name()));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenLoadBalancerResourceExistsAlreadyWithSameSchemeAndTargetGroupAndListener(boolean internal) {
        CloudStack stack = getCloudStack();
        AwsLoadBalancerScheme awsLoadBalancerScheme = internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbName = internal ? LB_NAME_INTERNAL : LB_NAME_EXTERNAL;
        CloudResource loadBalancerResource = CloudResource.builder()
                .withName(lbName)
                .withReference("aLoadBalancerArn")
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of(loadBalancerResource));
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, scheme, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder().listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResponse = CreateListenerResponse.builder().listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResponse);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        AwsLoadBalancerScheme awsLoadBalancerScheme = existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, schemeNew, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        String lbName = existingInternal ? LB_NAME_INTERNAL : LB_NAME_EXTERNAL;
        CloudResource loadBalancerResource = CloudResource.builder()
                .withName(lbName)
                .withReference("aLoadBalancerArn")
                .withType(ResourceType.ELASTIC_LOAD_BALANCER)
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of(loadBalancerResource));
        LoadBalancer loadBalancer = LoadBalancer.builder().
                loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResult = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, schemeNew, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder()
                .targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResult = CreateTargetGroupResponse.builder()
                .targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder()
                .listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResult = CreateListenerResponse.builder()
                .listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(existingInternal ? LoadBalancerTypeAttribute.PUBLIC : LoadBalancerTypeAttribute.PRIVATE);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenTargetGroupResourceExistsAlreadyWithSameSchemeAndLoadBalancerAndListener(boolean internal) {
        CloudStack stack = getCloudStack();
        AwsLoadBalancerScheme awsLoadBalancerScheme = internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, scheme, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);

        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
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
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, scheme, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder().listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResponse = CreateListenerResponse.builder().listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResponse);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());
        when(awsNativeLoadBalancerSecurityGroupProvider.getSecurityGroups(anyLong(), any()))
                .thenReturn(List.of());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(internal ? LoadBalancerTypeAttribute.PRIVATE : LoadBalancerTypeAttribute.PUBLIC);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenTargetGroupResourceExistsAlreadyButDifferentSchemeAndLoadBalancerAndListener(boolean existingInternal) {
        CloudStack stack = getCloudStack();
        AwsLoadBalancerScheme awsLoadBalancerScheme = existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, schemeNew, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder().loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResponse = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResponse);
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
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, schemeNew, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        TargetGroup targetGroup = TargetGroup.builder()
                .targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResult = CreateTargetGroupResponse.builder()
                .targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID)).thenReturn(List.of());
        Listener listener = Listener.builder()
                .listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResult = CreateListenerResponse.builder()
                .listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(existingInternal ? LoadBalancerTypeAttribute.PUBLIC : LoadBalancerTypeAttribute.PRIVATE);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenListenerResourceExistsAlreadyWithSameSchemeAndLoadBalancerAndTargetGroup(boolean internal) {
        CloudStack stack = getCloudStack();
        AwsLoadBalancerScheme awsLoadBalancerScheme = internal ? AwsLoadBalancerScheme.INTERNAL : AwsLoadBalancerScheme.INTERNET_FACING;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String scheme = internal ? INTERNAL : EXTERNAL;
        String lbNameNew = internal ? LB_NAME_INTERNAL_NEW : LB_NAME_EXTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, scheme, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder()
                .loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResult = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = internal ? TG_NAME_INTERNAL_NEW : TG_NAME_EXTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, scheme, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder().targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResponse = CreateTargetGroupResponse.builder().targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResponse);
        String listenerName = internal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(internal ? LoadBalancerTypeAttribute.PRIVATE : LoadBalancerTypeAttribute.PUBLIC);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesWhenListenerResourceExistsAlreadyButDifferentSchemeAndLoadBalancerAndTargetGroup(boolean existingInternal) {
        CloudStack stack = getCloudStack();
        AwsLoadBalancerScheme awsLoadBalancerScheme = existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(
                getAwsLoadBalancer(awsLoadBalancerScheme)));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, schemeNew, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder()
                .loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResult = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, schemeNew, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder()
                .targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResult = CreateTargetGroupResponse.builder()
                .targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        String listenerName = existingInternal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        Listener listener = Listener.builder()
                .listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResult = CreateListenerResponse.builder()
                .listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

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
        Set<LoadBalancerTypeAttribute> loadBalancerTypes = cloudResourceArgumentCaptor.getAllValues().stream()
                .filter(r -> ResourceType.ELASTIC_LOAD_BALANCER == r.getType())
                .map(r -> r.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class))
                .collect(Collectors.toSet());
        assertThat(loadBalancerTypes).containsExactly(existingInternal ? LoadBalancerTypeAttribute.PUBLIC : LoadBalancerTypeAttribute.PRIVATE);
    }

    @ParameterizedTest(name = "existingInternal: {0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesAwsTGNonSticky(boolean existingInternal) {
        AwsLoadBalancerScheme awsLoadBalancerScheme = existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
        AwsLoadBalancer awsLoadBalancer = getAwsLoadBalancer(awsLoadBalancerScheme);
        awsLoadBalancer.getListeners().forEach(awsListener -> awsListener.getTargetGroup().setStickySessionEnabled(false));
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(awsLoadBalancer));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, schemeNew, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder()
                .loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResult = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, schemeNew, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder()
                .targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResult = CreateTargetGroupResponse.builder()
                .targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        String listenerName = existingInternal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        Listener listener = Listener.builder()
                .listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResult = CreateListenerResponse.builder()
                .listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

        underTest.launchLoadBalancerResources(authenticatedContext, getCloudStack(), persistenceNotifier, loadBalancingClient, true);

        verify(loadBalancingClient, times(0)).modifyTargetGroupAttributes(any());
    }

    @ParameterizedTest(name = "existingInternal: {0}")
    @ValueSource(booleans = {false, true})
    void testLaunchLoadBalancerResourcesAwsTGSticky(boolean existingInternal) {
        AwsLoadBalancerScheme awsLoadBalancerScheme = existingInternal ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
        AwsLoadBalancer awsLoadBalancer = getAwsLoadBalancer(awsLoadBalancerScheme);
        awsLoadBalancer.getListeners().forEach(awsListener -> awsListener.getTargetGroup().setStickySessionEnabled(true));
        when(loadBalancerCommonService.getAwsLoadBalancers(any(), any(), any())).thenReturn(List.of(awsLoadBalancer));
        String schemeNew = existingInternal ? EXTERNAL : INTERNAL;
        String lbNameNew = existingInternal ? LB_NAME_EXTERNAL_NEW : LB_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancer(STACK_NAME, schemeNew, authenticatedContext.getCloudContext())).thenReturn(lbNameNew);
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER, STACK_ID)).thenReturn(List.of());
        LoadBalancer loadBalancer = LoadBalancer.builder()
                .loadBalancerArn("anARN").build();
        CreateLoadBalancerResponse loadBalancerResult = CreateLoadBalancerResponse.builder().loadBalancers(loadBalancer).build();
        when(loadBalancingClient.registerLoadBalancer(any())).thenReturn(loadBalancerResult);
        String tgNameNew = existingInternal ? TG_NAME_EXTERNAL_NEW : TG_NAME_INTERNAL_NEW;
        when(resourceNameService.loadBalancerTargetGroup(STACK_NAME, schemeNew, USER_FACING_PORT, authenticatedContext.getCloudContext()))
                .thenReturn(tgNameNew);
        when(resourceNameService.loadBalancerTargetGroupResourceTypeSchemeAndPortNamePart(eq(awsLoadBalancerScheme.resourceName()), eq(USER_FACING_PORT)))
                .thenReturn("TG" + USER_FACING_PORT + awsLoadBalancerScheme.resourceName());
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, STACK_ID)).thenReturn(List.of());
        TargetGroup targetGroup = TargetGroup.builder()
                .targetGroupArn("aTargetGroupArn").build();
        CreateTargetGroupResponse createTargetGroupResult = CreateTargetGroupResponse.builder()
                .targetGroups(List.of(targetGroup)).build();
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        when(loadBalancingClient.createTargetGroup(any())).thenReturn(createTargetGroupResult);
        String listenerName = existingInternal ? TG_NAME_INTERNAL : TG_NAME_EXTERNAL;
        CloudResource listenerResource = CloudResource.builder()
                .withName(listenerName)
                .withType(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER)
                .withReference("aListenerArn")
                .build();
        when(resourceRetriever
                .findAllByStatusAndTypeAndStack(CommonStatus.CREATED, ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, STACK_ID))
                .thenReturn(List.of(listenerResource));
        Listener listener = Listener.builder()
                .listenerArn("aListenerArn").build();
        CreateListenerResponse createListenerResult = CreateListenerResponse.builder()
                .listeners(listener).build();
        when(loadBalancingClient.registerListener(any())).thenReturn(createListenerResult);
        when(loadBalancingClient.registerTargets(any())).thenReturn(RegisterTargetsResponse.builder().build());

        underTest.launchLoadBalancerResources(authenticatedContext, getCloudStack(), persistenceNotifier, loadBalancingClient, true);

        verify(loadBalancingClient, times(0)).modifyTargetGroupAttributes(any());
    }

    private CloudStack getCloudStack() {
        Network network = new Network(new Subnet("10.0.0.0/16"), Map.of(NetworkConstants.VPC_ID, "vpc-avpcarn"));
        return CloudStack.builder()
                .network(network)
                .build();
    }

    private AwsLoadBalancer getAwsLoadBalancer() {
        return getAwsLoadBalancer(AwsLoadBalancerScheme.INTERNAL);
    }

    private AwsLoadBalancer getAwsLoadBalancer(AwsLoadBalancerScheme scheme) {
        AwsLoadBalancer awsLoadBalancer = new AwsLoadBalancer(scheme);
        HealthProbeParameters healthProbe = new HealthProbeParameters("/lb-healthcheck", 5030, NetworkProtocol.HTTPS, 10, 2);
        awsLoadBalancer.getOrCreateListener(USER_FACING_PORT, ProtocolEnum.TCP, healthProbe);
        return awsLoadBalancer;
    }

    private boolean resourceExistsWithTypeAndStatus(List<CloudResourceStatus> statuses, ResourceType resourceType, ResourceStatus resourceStatus) {
        return statuses
                .stream()
                .anyMatch(status -> resourceType.equals(status.getCloudResource().getType()) && resourceStatus.equals(status.getStatus()));
    }

    private String getLbName(AwsLoadBalancerScheme awsLoadBalancerScheme) {
        switch (awsLoadBalancerScheme) {
            case INTERNET_FACING:
                return LB_NAME_EXTERNAL_NEW;
            case GATEWAY_PRIVATE:
                return LB_NAME_GWAYPRIV_NEW;
            case INTERNAL:
                return LB_NAME_INTERNAL_NEW;
            default:
                throw new IllegalStateException("Unexpected value: " + awsLoadBalancerScheme);
        }
    }

    private String getTgName(AwsLoadBalancerScheme awsLoadBalancerScheme) {
        switch (awsLoadBalancerScheme) {
            case INTERNET_FACING:
                return TG_NAME_EXTERNAL_NEW;
            case GATEWAY_PRIVATE:
                return TG_NAME_GWAYPRIV_NEW;
            case INTERNAL:
                return TG_NAME_INTERNAL_NEW;
            default:
                throw new IllegalStateException("Unexpected value: " + awsLoadBalancerScheme);
        }
    }
}
