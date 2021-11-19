package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsUpscaleServiceTest {

    @Spy
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsAutoScalingService awsAutoScalingService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsMetadataCollector awsMetadataCollector;

    @InjectMocks
    private AwsUpscaleService awsUpscaleService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void upscaleTest() throws AmazonAutoscalingFailed {
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(
                        newAutoScalingGroup("masterASG", List.of("i-master1", "i-master2")),
                        newAutoScalingGroup("workerASG", List.of("i-worker1", "i-worker2", "i-worker3")));

        when(amazonAutoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationClient);
        when(awsClient.createEc2Client(any(), any())).thenReturn(mock(AmazonEc2Client.class));

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("worker")))
                .thenReturn("workerASG");

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        List<CloudResource> allInstances = new ArrayList<>();
        allInstances.add(newInstanceResource("worker1", "worker", "i-worker1"));
        allInstances.add(newInstanceResource("worker2", "worker", "i-worker2"));
        allInstances.add(newInstanceResource("worker3", "worker", "i-worker3"));
        CloudResource workerInstance4 = newInstanceResource("worker4", "worker", "i-worker4");
        allInstances.add(workerInstance4);
        CloudResource workerInstance5 = newInstanceResource("worker5", "worker", "i-worker5");
        allInstances.add(workerInstance5);
        when(cfStackUtil.getInstanceCloudResources(eq(authenticatedContext), eq(amazonCloudFormationClient), eq(amazonAutoScalingClient), anyList()))
                .thenReturn(allInstances);

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        groups.add(getMasterGroup(instanceAuthentication));

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        Map<String, String> tags = new HashMap<>();
        tags.put("owner", "cbuser");
        tags.put("created", "yesterday");
        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), tags, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);

        List<CloudResource> cloudResourceList = Collections.emptyList();
        awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList);
        verify(awsAutoScalingService, times(1)).updateAutoscalingGroup(any(AmazonAutoScalingClient.class), eq("workerASG"), eq(5));
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(List.of(worker)), eq(authenticatedContext),
                eq(amazonCloudFormationClient), any(), any());
        verify(awsAutoScalingService, times(1)).suspendAutoScaling(eq(authenticatedContext), eq(cloudStack));
        ArgumentCaptor<List<CloudResource>> captor = ArgumentCaptor.forClass(List.class);
        verify(awsComputeResourceService, times(1))
                .buildComputeResourcesForUpscale(eq(authenticatedContext), eq(cloudStack), anyList(), captor.capture(), any(), any());
        verify(awsTaggingService, times(1)).tagRootVolumes(eq(authenticatedContext), any(AmazonEc2Client.class), eq(allInstances), eq(tags));
        verify(awsCloudWatchService, times(1)).addCloudWatchAlarmsForSystemFailures(any(), eq("eu-west-1"),
                any(AwsCredentialView.class));
        List<CloudResource> newInstances = captor.getValue();
        assertEquals("Two new instances should be created", 2, newInstances.size());
        assertThat(newInstances, hasItem(workerInstance4));
        assertThat(newInstances, hasItem(workerInstance5));
        verify(cfStackUtil, times(0)).addLoadBalancerTargets(any(), any(), any());
    }

    @Test
    void upscaleAwsASGroupFail() throws AmazonAutoscalingFailed {
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);

        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(List.of("workerASG"));
        DescribeAutoScalingGroupsResult describeScaledAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(newAutoScalingGroup("workerASG", List.of("i-worker1", "i-worker2", "i-worker3")));
        when(amazonAutoScalingClient.describeAutoScalingGroups(eq(request)))
                .thenReturn(describeScaledAutoScalingGroupsResult);

        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationClient);

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("worker")))
                .thenReturn("workerASG");

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        groups.add(getMasterGroup(instanceAuthentication));

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);

        List<CloudResource> cloudResourceList = Collections.emptyList();

        AutoScalingGroup newWorkerASGroup = newAutoScalingGroup("workerASG",
                List.of("i-worker1", "i-worker2", "i-worker3", "i-worker4", "i-worker5"));

        when(awsAutoScalingService.getAutoscalingGroups(eq(amazonAutoScalingClient), any()))
                .thenReturn(Collections.singletonList(newWorkerASGroup));

        doThrow(new AmazonAutoscalingFailed("autoscaling failed"))
                .when(awsAutoScalingService).scheduleStatusChecks(
                eq(List.of(worker)), eq(authenticatedContext), eq(amazonCloudFormationClient), any(Date.class), any());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList));
        Assertions.assertEquals("Autoscaling group update failed: Amazon Autoscaling Group was not able to reach the desired state (3 instances instead of 5). "
                + "Original autoscaling group state has been recovered. Failure reason: autoscaling failed", exception.getMessage());

        verify(awsAutoScalingService, times(1)).updateAutoscalingGroup(any(AmazonAutoScalingClient.class), eq("workerASG"), eq(5));
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(List.of(worker)), eq(authenticatedContext),
                eq(amazonCloudFormationClient), any(), any());
        verify(awsComputeResourceService, times(0)).buildComputeResourcesForUpscale(eq(authenticatedContext), eq(cloudStack),
                anyList(), anyList(), any(), any());
        verify(awsAutoScalingService, times(1)).suspendAutoScaling(eq(authenticatedContext), eq(cloudStack));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingClient), eq("i-worker4"));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingClient), eq("i-worker5"));
    }

    @Test
    void upscaleAwsVolumeFail() throws AmazonAutoscalingFailed {
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);

        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        request.setAutoScalingGroupNames(new ArrayList<>(Collections.singletonList("workerASG")));
        DescribeAutoScalingGroupsResult describeScaledAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(newAutoScalingGroup("workerASG", List.of("i-worker1", "i-worker2", "i-worker3")));
        when(amazonAutoScalingClient.describeAutoScalingGroups(eq(request)))
                .thenReturn(describeScaledAutoScalingGroupsResult);

        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationClient);

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("worker")))
                .thenReturn("workerASG");

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        List<CloudResource> allInstances = new ArrayList<>();
        allInstances.add(newInstanceResource("worker1", "worker", "i-worker1"));
        allInstances.add(newInstanceResource("worker2", "worker", "i-worker2"));
        allInstances.add(newInstanceResource("worker3", "worker", "i-worker3"));
        CloudResource workerInstance4 = newInstanceResource("worker4", "worker", "i-worker4");
        allInstances.add(workerInstance4);
        CloudResource workerInstance5 = newInstanceResource("worker5", "worker", "i-worker5");
        allInstances.add(workerInstance5);
        when(cfStackUtil.getInstanceCloudResources(eq(authenticatedContext), eq(amazonCloudFormationClient), eq(amazonAutoScalingClient), anyList()))
                .thenReturn(allInstances);

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        groups.add(getMasterGroup(instanceAuthentication));

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);

        List<CloudResource> cloudResourceList = Collections.emptyList();

        AutoScalingGroup newWorkerASGroup = newAutoScalingGroup("workerASG",
                List.of("i-worker1", "i-worker2", "i-worker3", "i-worker4", "i-worker5"));

        when(awsAutoScalingService.getAutoscalingGroups(eq(amazonAutoScalingClient), any()))
                .thenReturn(List.of(newWorkerASGroup));

        when(awsComputeResourceService.buildComputeResourcesForUpscale(any(), any(), anyList(), anyList(), anyList(), anyList()))
                .thenThrow(new CloudConnectorException("volume create error"));

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList));
        Assertions.assertEquals("Failed to create some resource on AWS for upscaled nodes, please check your quotas on AWS. " +
                "Original autoscaling group state has been recovered. Exception: volume create error", exception.getMessage());

        verify(awsAutoScalingService, times(1)).updateAutoscalingGroup(any(AmazonAutoScalingClient.class), eq("workerASG"), eq(5));
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(List.of(worker)), eq(authenticatedContext),
                eq(amazonCloudFormationClient), any(), any());
        verify(awsComputeResourceService, times(1)).buildComputeResourcesForUpscale(eq(authenticatedContext), eq(cloudStack),
                anyList(), anyList(), any(), any());
        verify(awsAutoScalingService, times(2)).suspendAutoScaling(eq(authenticatedContext), eq(cloudStack));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingClient), eq("i-worker4"));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingClient), eq("i-worker5"));
        verify(cfStackUtil, times(0)).addLoadBalancerTargets(any(), any(), any());
    }

    @Test
    void upscaleWithLoadBalancers() {
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(new DescribeAutoScalingGroupsResult()
                        .withAutoScalingGroups(
                                newAutoScalingGroup("masterASG", List.of("i-master1", "i-master2")),
                                newAutoScalingGroup("workerASG", List.of("i-worker1", "i-worker2", "i-worker3"))));
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationClient);
        when(awsClient.createEc2Client(any(), any())).thenReturn(mock(AmazonEc2Client.class));

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("worker")))
                .thenReturn("workerASG");

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        List<CloudResource> allInstances = List.of(
                newInstanceResource("worker1", "worker", "i-worker1"),
                newInstanceResource("worker2", "worker", "i-worker2"),
                newInstanceResource("worker3", "worker", "i-worker3"),
                newInstanceResource("worker4", "worker", "i-worker4"),
                newInstanceResource("worker5", "worker", "i-worker5"));
        when(cfStackUtil.getInstanceCloudResources(eq(authenticatedContext), eq(amazonCloudFormationClient), eq(amazonAutoScalingClient), anyList()))
                .thenReturn(allInstances);
        doNothing().when(cfStackUtil).addLoadBalancerTargets(any(), any(), any());

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        List<CloudLoadBalancer> loadBalancers = List.of(
                new CloudLoadBalancer(LoadBalancerType.PRIVATE),
                new CloudLoadBalancer(LoadBalancerType.PUBLIC)
        );

        Group master = getMasterGroup(instanceAuthentication);
        groups.add(master);

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        Map<String, String> tags = new HashMap<>();
        tags.put("owner", "cbuser");
        tags.put("created", "yesterday");
        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), tags, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null, loadBalancers);

        List<CloudResource> cloudResourceList = Collections.emptyList();
        awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList);

        verify(cfStackUtil, times(2)).addLoadBalancerTargets(any(), any(), any());
    }

    private Group getWorkerGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        InstanceTemplate newInstanceTemplate = mock(InstanceTemplate.class);
        when(newInstanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        CloudInstance workerInstance4 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance5 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        cloudInstances.add(workerInstance4);
        cloudInstances.add(workerInstance5);
        return new Group("worker", InstanceGroupType.CORE, cloudInstances, null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), 50, Optional.empty(), createGroupNetwork(), emptyMap());
    }

    private Group getMasterGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> masterInstances = new ArrayList<>();
        CloudInstance masterInstance1 = new CloudInstance("i-master1", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance masterInstance2 = new CloudInstance("i-master2", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        masterInstances.add(masterInstance1);
        masterInstances.add(masterInstance2);
        return new Group("master", InstanceGroupType.GATEWAY, masterInstances, null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), 50, Optional.empty(), createGroupNetwork(), emptyMap());
    }

    private Network getNetwork() {
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        return new Network(new Subnet(null), networkParameters);
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

    private AutoScalingGroup newAutoScalingGroup(String groupName, List<String> instances) {
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setAutoScalingGroupName(groupName);
        autoScalingGroup.setInstances(instances.stream().map(instance -> new Instance().withInstanceId(instance)).collect(Collectors.toList()));
        return autoScalingGroup;
    }

    private CloudResource newInstanceResource(String name, String group, String instanceId) {
        return CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name(name).group(group).instanceId(instanceId).build();
    }
}