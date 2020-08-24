package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsDownscaleServiceTest {

    @InjectMocks
    private AwsDownscaleService underTest;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private AwsClient awsClient;

    @Test
    void downscaleASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication);
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "crn", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a"), new HashMap<>()), "1", "1"),

                new CloudCredential());
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of(new Instance().withInstanceId("i-worker1")));
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingRetryClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
                .thenReturn(new DetachInstancesResult());

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        List<DetachInstancesRequest> allValues = detachInstancesRequestArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).getInstanceIds(), contains("i-worker1"));
        verify(amazonAutoScalingRetryClient, times(1)).detachInstances(any());
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        assertEquals(describeAutoScalingGroupsRequest.getValue().getAutoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    void downscaleASGWhenAllInstancesHaveBeenRemovedFromASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication);
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication);
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "crn", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a"), new HashMap<>()), "1", "1"),
                new CloudCredential());
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of());
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingRetryClient, never()).detachInstances(any());
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        assertEquals(describeAutoScalingGroupsRequest.getValue().getAutoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    void downscaleOrderTest() {
        // We need to invoke  detach, terminate and update ASG in this order othervise a strange sporadic concurrency issue can occure on AWS side

        // The proper order is:
        //amazonASClient.detachInstances(...);
        //amazonEC2Client.terminateInstances(...);
        //amazonASClient.updateAutoScalingGroup(...);

        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication);
        cloudInstances.add(workerInstance1);
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "crn", "AWS", "AWS",
                Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a"), new HashMap<>()), "1", "1"),
                new CloudCredential());

        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of(new Instance().withInstanceId("i-worker1")));
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);

        when(amazonAutoScalingRetryClient.detachInstances(any())).thenReturn(new DetachInstancesResult());
        when(amazonEC2Client.terminateInstances(any())).thenReturn(new TerminateInstancesResult());
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        //create inOrder object passing any mocks that need to be verified in order
        InOrder inOrder = Mockito.inOrder(amazonAutoScalingRetryClient, amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        // Following will make sure that detach, ivoked before terminate and terminate invoked before update ASG!
        inOrder.verify(amazonAutoScalingRetryClient).detachInstances(any());
        inOrder.verify(amazonEC2Client).terminateInstances(any());
        inOrder.verify(amazonAutoScalingRetryClient).updateAutoScalingGroup(any());
    }

    @Test
    void downscaleWithLoadBalancers() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = Collections.emptyList();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = List.of(new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication));
        CloudLoadBalancer privateLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        CloudLoadBalancer publicLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);

        AuthenticatedContext authenticatedContext = new AuthenticatedContext(new CloudContext(1L, "teststack", "crn", "AWS", "AWS",
            Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a"), new HashMap<>()), "1", "1"),
            new CloudCredential());
        AmazonAutoScalingRetryClient amazonAutoScalingRetryClient = mock(AmazonAutoScalingRetryClient.class);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");
        when(stack.getLoadBalancers()).thenReturn(List.of(privateLoadBalancer, publicLoadBalancer));
        doNothing().when(cfStackUtil).removeLoadBalancerTargets(any(), any(), any());

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of(new Instance().withInstanceId("i-worker1")));
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
            .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingRetryClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
            .thenReturn(new DetachInstancesResult());

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(cfStackUtil, times(2)).removeLoadBalancerTargets(any(), any(), any());
    }

}