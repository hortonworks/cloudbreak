package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
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
    private AwsCloudFormationClient awsClient;

    @Test
    void downscaleASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());
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
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
                .thenReturn(new DetachInstancesResult());
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        List<DetachInstancesRequest> allValues = detachInstancesRequestArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).getInstanceIds(), contains("i-worker1"));
        verify(amazonAutoScalingClient, times(1)).detachInstances(any());
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        assertEquals(describeAutoScalingGroupsRequest.getValue().getAutoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    private void mockDescribeInstances(AmazonEc2Client amazonEC2Client) {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenAnswer(a -> {
            DescribeInstancesRequest request = a.getArgument(0, DescribeInstancesRequest.class);
            List<com.amazonaws.services.ec2.model.Instance> instances = request.getInstanceIds().stream()
                    .map(i -> new com.amazonaws.services.ec2.model.Instance().withInstanceId(i))
                    .collect(Collectors.toList());
            return new DescribeInstancesResult().withReservations(new Reservation().withInstances(instances));
        });
    }

    @Test
    void downscaleASGWhenAllInstancesHaveBeenRemovedFromASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
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
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
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
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());

        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of(new Instance().withInstanceId("i-worker1")));
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);
        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);

        when(amazonAutoScalingClient.detachInstances(any())).thenReturn(new DetachInstancesResult());
        when(amazonEC2Client.terminateInstances(any())).thenReturn(new TerminateInstancesResult());
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");
        mockDescribeInstances(amazonEC2Client);

        //create inOrder object passing any mocks that need to be verified in order
        InOrder inOrder = Mockito.inOrder(amazonAutoScalingClient, amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        // Following will make sure that detach, ivoked before terminate and terminate invoked before update ASG!
        inOrder.verify(amazonAutoScalingClient).detachInstances(any());
        inOrder.verify(amazonEC2Client).terminateInstances(any());
        inOrder.verify(amazonAutoScalingClient).updateAutoScalingGroup(any());
    }

    @Test
    void downscaleWithLoadBalancers() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = Collections.emptyList();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        List<CloudInstance> cloudInstances = List.of(new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1"));
        CloudLoadBalancer privateLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PRIVATE);
        CloudLoadBalancer publicLoadBalancer = new CloudLoadBalancer(LoadBalancerType.PUBLIC);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
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
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
            .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
            .thenReturn(new DetachInstancesResult());
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(cfStackUtil, times(2)).removeLoadBalancerTargets(any(), any(), any());
    }

    @Test
    public void downscaleButInstanceNotFoundOnAWS() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-4").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance4 = new CloudInstance("i-worker4", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        cloudInstances.add(workerInstance4);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        AmazonEC2Waiters amazonEC2Waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(amazonEC2Waiters);
        Waiter waiter = mock(Waiter.class);

        when(amazonEC2Waiters.instanceTerminated()).thenReturn(waiter);
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        AmazonServiceException amazonServiceException = new AmazonServiceException("Cannot execute method: terminateInstances. Invalid id: " +
                "\"i-worker1\",\"i-worker2\"");
        amazonServiceException.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        when(amazonEC2Client.terminateInstances(any())).thenThrow(amazonServiceException).thenReturn(new TerminateInstancesResult());

        AmazonServiceException amazonServiceExceptionForWaiter = new AmazonServiceException("Cannot execute method: terminateInstances. Invalid id: " +
                "\"i-worker3\"");
        amazonServiceExceptionForWaiter.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        doThrow(amazonServiceExceptionForWaiter).doNothing().when(waiter).run(any());

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of());
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        verify(amazonEC2Client, times(2)).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        List<TerminateInstancesRequest> allValues = terminateInstancesRequestArgumentCaptor.getAllValues();
        List<String> firstTerminateInstanceIds = allValues.get(0).getInstanceIds();
        assertEquals(4, firstTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker1"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker2"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        List<String> secondTerminateInstanceIds = allValues.get(1).getInstanceIds();
        assertEquals(2, secondTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        ArgumentCaptor<WaiterParameters> waiterParametersArgumentCaptor = ArgumentCaptor.forClass(WaiterParameters.class);

        verify(waiter, times(2)).run(waiterParametersArgumentCaptor.capture());

        List<WaiterParameters> waiterParametersList = waiterParametersArgumentCaptor.getAllValues();

        DescribeInstancesRequest firstDescribeInstancesRequest = (DescribeInstancesRequest) waiterParametersList.get(0).getRequest();
        assertEquals(2, firstDescribeInstancesRequest.getInstanceIds().size());
        assertTrue(firstDescribeInstancesRequest.getInstanceIds().contains("i-worker3"));
        assertTrue(firstDescribeInstancesRequest.getInstanceIds().contains("i-worker4"));

        DescribeInstancesRequest secondDescribeInstancesRequest = (DescribeInstancesRequest) waiterParametersList.get(1).getRequest();
        assertEquals(1, secondDescribeInstancesRequest.getInstanceIds().size());
        assertTrue(secondDescribeInstancesRequest.getInstanceIds().contains("i-worker4"));

        assertEquals(describeAutoScalingGroupsRequest.getValue().getAutoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    public void downscaleNoWaiterIfNoInstanceLeftOnAWS() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(new CloudResource.Builder().name("i-1").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-2").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-3").type(ResourceType.AWS_INSTANCE).build(),
                new CloudResource.Builder().name("i-4").type(ResourceType.AWS_INSTANCE).build());
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<CloudInstance> cloudInstances = new ArrayList<>();
        InstanceTemplate workerTemplate = mock(InstanceTemplate.class);
        when(workerTemplate.getGroupName()).thenReturn("worker");
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance4 = new CloudInstance("i-worker4", workerTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        cloudInstances.add(workerInstance4);
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, new CloudCredential());
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        Waiter waiter = mock(Waiter.class);
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        AmazonServiceException amazonServiceException = new AmazonServiceException("Cannot execute method: terminateInstances. Invalid id: " +
                "\"i-worker1\",\"i-worker2\",\"i-worker3\",\"i-worker4\"");
        amazonServiceException.setErrorCode(INSTANCE_NOT_FOUND_ERROR_CODE);
        when(amazonEC2Client.terminateInstances(any())).thenThrow(amazonServiceException).thenReturn(new TerminateInstancesResult());

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of());
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
        verify(cfStackUtil, times(0)).removeLoadBalancerTargets(any(), any(), any());

        ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        verify(amazonEC2Client, times(1)).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        List<TerminateInstancesRequest> allValues = terminateInstancesRequestArgumentCaptor.getAllValues();
        List<String> firstTerminateInstanceIds = allValues.get(0).getInstanceIds();
        assertEquals(4, firstTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker1"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker2"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        verify(waiter, times(0)).run(any());
    }

}