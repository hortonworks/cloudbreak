package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
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
import com.sequenceiq.common.api.type.ResourceType;

class AwsUpscaleServiceTest {

    @Spy
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AwsClient awsClient;

    @Mock
    private AwsAutoScalingService awsAutoScalingService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @InjectMocks
    private AwsUpscaleService awsUpscaleService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void upscaleTest() throws AmazonAutoscalingFailed {
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        AmazonCloudFormationRetryClient amazonCloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        List<AutoScalingGroup> autoScalingGroups = new ArrayList<>();

        AutoScalingGroup masterASGroup = new AutoScalingGroup();
        masterASGroup.setAutoScalingGroupName("masterASG");
        List<Instance> masterASGInstances = new ArrayList<>();
        masterASGInstances.add(new Instance().withInstanceId("i-master1"));
        masterASGInstances.add(new Instance().withInstanceId("i-master2"));
        masterASGroup.setInstances(masterASGInstances);

        AutoScalingGroup workerASGroup = new AutoScalingGroup();
        workerASGroup.setAutoScalingGroupName("workerASG");
        List<Instance> workerASGInstances = new ArrayList<>();
        workerASGInstances.add(new Instance().withInstanceId("i-worker1"));
        workerASGInstances.add(new Instance().withInstanceId("i-worker2"));
        workerASGInstances.add(new Instance().withInstanceId("i-worker3"));
        workerASGroup.setInstances(workerASGInstances);

        autoScalingGroups.add(masterASGroup);
        autoScalingGroups.add(workerASGroup);

        describeAutoScalingGroupsResult.setAutoScalingGroups(autoScalingGroups);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingRetryClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingRetryClient);
        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationRetryClient);

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationRetryClient.class), eq("worker")))
                .thenReturn("workerASG");
        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationRetryClient.class), eq("master")))
                .thenReturn("masterASG");

        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")), "1", "1"), new CloudCredential());

        ArrayList<CloudResource> allInstances = new ArrayList<>();
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker1").group("worker").instanceId("i-worker1").build());
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker2").group("worker").instanceId("i-worker2").build());
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker3").group("worker").instanceId("i-worker3").build());
        CloudResource workerInstance4 = CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker4").group("worker").instanceId("i-worker4").build();
        allInstances.add(workerInstance4);
        CloudResource workerInstance5 = CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker5").group("worker").instanceId("i-worker5").build();
        allInstances.add(workerInstance5);
        when(cfStackUtil.getInstanceCloudResources(eq(authenticatedContext), eq(amazonCloudFormationRetryClient), eq(amazonAutoScalingRetryClient), anyList()))
                .thenReturn(allInstances);

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        Group master = getMasterGroup(instanceAuthentication);
        groups.add(master);

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);

        List<CloudResource> cloudResourceList = Collections.emptyList();
        awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList);
        verify(awsAutoScalingService, times(1)).updateAutoscalingGroup(any(AmazonAutoScalingRetryClient.class), eq("workerASG"), eq(5));
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(cloudStack), eq(authenticatedContext),  eq(amazonCloudFormationRetryClient), any());
        verify(awsAutoScalingService, times(1)).suspendAutoScaling(eq(authenticatedContext), eq(cloudStack));
        ArgumentCaptor<List<CloudResource>> captor = ArgumentCaptor.forClass(List.class);
        verify(awsComputeResourceService, times(1))
                .buildComputeResourcesForUpscale(eq(authenticatedContext), eq(cloudStack), anyList(), captor.capture(), any(), any());
        List<CloudResource> newInstances = captor.getValue();
        assertEquals("Two new instances should be created", 2, newInstances.size());
        assertThat(newInstances, hasItem(workerInstance4));
        assertThat(newInstances, hasItem(workerInstance5));
    }

    @Test
    void upscaleAwsASGroupFail() throws AmazonAutoscalingFailed {
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        AmazonCloudFormationRetryClient amazonCloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        List<AutoScalingGroup> autoScalingGroups = new ArrayList<>();

        AutoScalingGroup masterASGroup = new AutoScalingGroup();
        masterASGroup.setAutoScalingGroupName("masterASG");
        List<Instance> masterASGInstances = new ArrayList<>();
        masterASGInstances.add(new Instance().withInstanceId("i-master1"));
        masterASGInstances.add(new Instance().withInstanceId("i-master2"));
        masterASGroup.setInstances(masterASGInstances);

        AutoScalingGroup workerASGroup = new AutoScalingGroup();
        workerASGroup.setAutoScalingGroupName("workerASG");
        List<Instance> workerASGInstances = new ArrayList<>();
        workerASGInstances.add(new Instance().withInstanceId("i-worker1"));
        workerASGInstances.add(new Instance().withInstanceId("i-worker2"));
        workerASGInstances.add(new Instance().withInstanceId("i-worker3"));
        workerASGroup.setInstances(workerASGInstances);

        autoScalingGroups.add(masterASGroup);
        autoScalingGroups.add(workerASGroup);

        describeAutoScalingGroupsResult.setAutoScalingGroups(autoScalingGroups);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingRetryClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingRetryClient);
        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationRetryClient);

        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationRetryClient.class), eq("worker")))
                .thenReturn("workerASG");
        when(cfStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationRetryClient.class), eq("master")))
                .thenReturn("masterASG");

        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")), "1", "1"), new CloudCredential());

        ArrayList<CloudResource> allInstances = new ArrayList<>();
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker1").group("worker").instanceId("i-worker1").build());
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker2").group("worker").instanceId("i-worker2").build());
        allInstances.add(CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker3").group("worker").instanceId("i-worker3").build());
        CloudResource workerInstance4 = CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker4").group("worker").instanceId("i-worker4").build();
        allInstances.add(workerInstance4);
        CloudResource workerInstance5 = CloudResource.builder().type(ResourceType.AWS_INSTANCE).status(CommonStatus.CREATED)
                .name("worker5").group("worker").instanceId("i-worker5").build();
        allInstances.add(workerInstance5);
        when(cfStackUtil.getInstanceCloudResources(eq(authenticatedContext), eq(amazonCloudFormationRetryClient), eq(amazonAutoScalingRetryClient), anyList()))
                .thenReturn(allInstances);

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        Group master = getMasterGroup(instanceAuthentication);
        groups.add(master);

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        CloudStack cloudStack = new CloudStack(groups, getNetwork(), null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);

        List<CloudResource> cloudResourceList = Collections.emptyList();

        AutoScalingGroup newWorkerASGroup = new AutoScalingGroup();
        newWorkerASGroup.setAutoScalingGroupName("workerASG");
        List<Instance> newWorkerASGInstances = new ArrayList<>();
        newWorkerASGInstances.add(new Instance().withInstanceId("i-worker1"));
        newWorkerASGInstances.add(new Instance().withInstanceId("i-worker2"));
        newWorkerASGInstances.add(new Instance().withInstanceId("i-worker3"));
        newWorkerASGInstances.add(new Instance().withInstanceId("i-worker4"));
        newWorkerASGInstances.add(new Instance().withInstanceId("i-worker5"));
        newWorkerASGroup.setInstances(newWorkerASGInstances);

        when(awsAutoScalingService.getAutoscalingGroups(eq(amazonAutoScalingRetryClient), any()))
                .thenReturn(Collections.singletonList(newWorkerASGroup));

        doThrow(new AmazonAutoscalingFailed("autoscaling failed"))
                .when(awsAutoScalingService).scheduleStatusChecks(eq(cloudStack),
                eq(authenticatedContext), eq(amazonCloudFormationRetryClient), any(Date.class));

        assertThrows(CloudConnectorException.class, () -> awsUpscaleService.upscale(authenticatedContext, cloudStack, cloudResourceList),
                "Autoscaling group update failed: 'autoscaling failed' Original autoscaling group state has been recovered.");
        verify(awsAutoScalingService, times(1)).updateAutoscalingGroup(any(AmazonAutoScalingRetryClient.class), eq("workerASG"), eq(5));
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(cloudStack), eq(authenticatedContext),  eq(amazonCloudFormationRetryClient), any());
        verify(awsComputeResourceService, times(0)).buildComputeResourcesForUpscale(eq(authenticatedContext), eq(cloudStack),
                anyList(), anyList(), any(), any());
        verify(awsAutoScalingService, times(1)).suspendAutoScaling(eq(authenticatedContext), eq(cloudStack));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingRetryClient), eq("i-worker4"));
        verify(awsAutoScalingService, times(1)).terminateInstance(eq(amazonAutoScalingRetryClient), eq("i-worker5"));
        Map<String, Integer> desiredGroups = new HashMap<>();
        desiredGroups.put("workerASG", 3);
        desiredGroups.put("masterASG", 2);
        verify(awsAutoScalingService, times(1)).scheduleStatusChecks(eq(desiredGroups), eq(authenticatedContext), any(Date.class));
    }

    private Group getWorkerGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication);
        InstanceTemplate newInstanceTemplate = mock(InstanceTemplate.class);
        when(newInstanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        CloudInstance workerInstance4 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication);
        CloudInstance workerInstance5 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication);
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        cloudInstances.add(workerInstance4);
        cloudInstances.add(workerInstance5);
        return new Group("worker", InstanceGroupType.CORE, cloudInstances, null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50, Optional.empty());
    }

    private Group getMasterGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> masterInstances = new ArrayList<>();
        CloudInstance masterInstance1 = new CloudInstance("i-master1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance masterInstance2 = new CloudInstance("i-master2", mock(InstanceTemplate.class), instanceAuthentication);
        masterInstances.add(masterInstance1);
        masterInstances.add(masterInstance2);
        return new Group("master", InstanceGroupType.GATEWAY, masterInstances, null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50, Optional.empty());
    }

    private Network getNetwork() {
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        return new Network(new Subnet(null), networkParameters);
    }
}