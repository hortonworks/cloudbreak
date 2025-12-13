package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.LoadBalancerService;
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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.polling.Poller;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;

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

    @Mock
    private LoadBalancerService loadBalancerService;

    @Mock
    private Ec2Waiter ec2Waiter;

    @Mock
    private Poller<Boolean> poller;

    @BeforeEach
    public void setup() {
        doAnswer(invocation -> ((AsgInstanceDetachWaiter) invocation.getArgument(2)).process())
                .when(poller).runPoller(nullable(Long.class), nullable(Long.class), any());
    }

    @Test
    void downscaleASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-2").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-3").withType(ResourceType.AWS_INSTANCE).build());
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
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(Instance.builder().instanceId("i-worker1").build())
                        .build())
                .build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
                .thenReturn(DetachInstancesResponse.builder().build());
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        List<DetachInstancesRequest> allValues = detachInstancesRequestArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).instanceIds(), contains("i-worker1"));
        verify(amazonAutoScalingClient, times(1)).detachInstances(any());
        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

        assertEquals(describeAutoScalingGroupsRequest.getValue().autoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    private void mockDescribeInstances(AmazonEc2Client amazonEC2Client) {
        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenAnswer(a -> {
            DescribeInstancesRequest request = a.getArgument(0, DescribeInstancesRequest.class);
            List<software.amazon.awssdk.services.ec2.model.Instance> instances = request.instanceIds().stream()
                    .map(i -> software.amazon.awssdk.services.ec2.model.Instance.builder().instanceId(i).build())
                    .collect(Collectors.toList());
            return DescribeInstancesResponse.builder().reservations(Reservation.builder().instances(instances).build()).build();
        });
    }

    @Test
    void downscaleASGWhenAllInstancesHaveBeenRemovedFromASG() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-2").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-3").withType(ResourceType.AWS_INSTANCE).build());
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
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(List.of()).build())
                        .build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

        assertEquals(describeAutoScalingGroupsRequest.getValue().autoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    void downscaleOrderTest() {
        // We need to invoke  detach, terminate and update ASG in this order othervise a strange sporadic concurrency issue can occure on AWS side

        // The proper order is:
        //amazonASClient.detachInstances(...);
        //amazonEC2Client.terminateInstances(...);
        //amazonASClient.updateAutoScalingGroup(...);

        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build());
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
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(Instance.builder().instanceId("i-worker1").build())
                        .build())
                .build();
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);

        when(amazonAutoScalingClient.detachInstances(any())).thenReturn(DetachInstancesResponse.builder().build());
        when(amazonEC2Client.terminateInstances(any())).thenReturn(TerminateInstancesResponse.builder().build());
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");
        mockDescribeInstances(amazonEC2Client);

        //create inOrder object passing any mocks that need to be verified in order
        InOrder inOrder = inOrder(amazonAutoScalingClient, amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);
        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

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
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");
        when(stack.getLoadBalancers()).thenReturn(List.of(privateLoadBalancer, publicLoadBalancer));
        doNothing().when(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(Instance.builder().instanceId("i-worker1").build())
                        .build())
                .build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        ArgumentCaptor<DetachInstancesRequest> detachInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DetachInstancesRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
            .thenReturn(describeAutoScalingGroupsResult);
        when(amazonAutoScalingClient.detachInstances(detachInstancesRequestArgumentCaptor.capture()))
            .thenReturn(DetachInstancesResponse.builder().build());
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());
    }

    @Test
    public void downscaleButInstanceNotFoundOnAWS() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(
                CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-2").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-3").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-4").withType(ResourceType.AWS_INSTANCE).build());
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
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        AwsServiceException amazonServiceException = AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder()
                .errorMessage("Cannot execute method: terminateInstances. Invalid id: " + "\"i-worker1\",\"i-worker2\"")
                .errorCode(INSTANCE_NOT_FOUND).build()).build();
        when(amazonEC2Client.terminateInstances(any())).thenThrow(amazonServiceException).thenReturn(TerminateInstancesResponse.builder().build());

        AwsServiceException amazonServiceExceptionForWaiter = AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder()
                .errorMessage("Cannot execute method: terminateInstances. Invalid id: " + "\"i-worker3\"")
                .errorCode(INSTANCE_NOT_FOUND).build()).build();
        when(amazonEC2Client.waiters()).thenReturn(ec2Waiter);
        doThrow(amazonServiceExceptionForWaiter).doReturn(null)
                .when(ec2Waiter).waitUntilInstanceTerminated(any(DescribeInstancesRequest.class), any());

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(List.of()).build())
                .build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

        ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        verify(amazonEC2Client, times(2)).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        List<TerminateInstancesRequest> allValues = terminateInstancesRequestArgumentCaptor.getAllValues();
        List<String> firstTerminateInstanceIds = allValues.get(0).instanceIds();
        assertEquals(4, firstTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker1"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker2"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        List<String> secondTerminateInstanceIds = allValues.get(1).instanceIds();
        assertEquals(2, secondTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        ArgumentCaptor<DescribeInstancesRequest> waiterParametersArgumentCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);

        verify(ec2Waiter, times(2)).waitUntilInstanceTerminated(waiterParametersArgumentCaptor.capture(), any());

        DescribeInstancesRequest firstDescribeInstancesRequest = waiterParametersArgumentCaptor.getAllValues().get(0);
        assertEquals(2, firstDescribeInstancesRequest.instanceIds().size());
        assertTrue(firstDescribeInstancesRequest.instanceIds().contains("i-worker3"));
        assertTrue(firstDescribeInstancesRequest.instanceIds().contains("i-worker4"));

        DescribeInstancesRequest secondDescribeInstancesRequest = waiterParametersArgumentCaptor.getAllValues().get(1);
        assertEquals(1, secondDescribeInstancesRequest.instanceIds().size());
        assertTrue(secondDescribeInstancesRequest.instanceIds().contains("i-worker4"));

        assertEquals(describeAutoScalingGroupsRequest.getValue().autoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    public void checkTerminationRecursionDoesNotCauseEndlessLoop() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-2").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-3").withType(ResourceType.AWS_INSTANCE).build(),
        CloudResource.builder().withName("i-4").withType(ResourceType.AWS_INSTANCE).build());
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

        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        AwsServiceException amazonServiceException = AwsServiceException.builder().awsErrorDetails(
                AwsErrorDetails.builder().errorMessage("Cannot execute method: terminateInstances. Invalid id: \"i-worker1\",\"i-worker2\"")
                        .errorCode(INSTANCE_NOT_FOUND)
                        .build()).build();
        when(amazonEC2Client.terminateInstances(any())).thenThrow(amazonServiceException);

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder().instances(List.of()).build()).build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResponse);
        mockDescribeInstances(amazonEC2Client);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.downscale(authenticatedContext, stack, resources, cloudInstances));

        assertEquals(cloudbreakServiceException.getMessage(), "AWS instance termination failed, instance termination list is not shrinking");

        verify(amazonAutoScalingClient, never()).detachInstances(any());

        ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        verify(amazonEC2Client, times(2)).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        List<TerminateInstancesRequest> allValues = terminateInstancesRequestArgumentCaptor.getAllValues();
        List<String> firstTerminateInstanceIds = allValues.get(0).instanceIds();
        assertEquals(4, firstTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker1"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker2"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        List<String> secondTerminateInstanceIds = allValues.get(1).instanceIds();
        assertEquals(2, secondTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));

        assertEquals(describeAutoScalingGroupsRequest.getValue().autoScalingGroupNames(), List.of("autoscalegroup-1"));
    }

    @Test
    public void downscaleNoWaiterIfNoInstanceLeftOnAWS() {
        CloudStack stack = mock(CloudStack.class);
        List<CloudResource> resources = List.of(CloudResource.builder().withName("i-1").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-2").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-3").withType(ResourceType.AWS_INSTANCE).build(),
                CloudResource.builder().withName("i-4").withType(ResourceType.AWS_INSTANCE).build());
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
        when(cfStackUtil.getAutoscalingGroupName(any(), (String) any(), any())).thenReturn("autoscalegroup-1");

        AwsServiceException amazonServiceException = AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder()
                .errorMessage("Cannot execute method: terminateInstances. Invalid id: " +
                "\"i-worker1\",\"i-worker2\",\"i-worker3\",\"i-worker4\"")
                .errorCode(INSTANCE_NOT_FOUND)
                .build()).build();

        when(amazonEC2Client.terminateInstances(any())).thenThrow(amazonServiceException).thenReturn(TerminateInstancesResponse.builder().build());

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(List.of())
                        .build())
                .build();
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequest = ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        when(amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest.capture()))
                .thenReturn(describeAutoScalingGroupsResult);
        mockDescribeInstances(amazonEC2Client);

        underTest.downscale(authenticatedContext, stack, resources, cloudInstances);

        verify(amazonAutoScalingClient, never()).detachInstances(any());
        verify(loadBalancerService).removeLoadBalancerTargets(any(), any(), any());

        ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        verify(amazonEC2Client, times(1)).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        List<TerminateInstancesRequest> allValues = terminateInstancesRequestArgumentCaptor.getAllValues();
        List<String> firstTerminateInstanceIds = allValues.get(0).instanceIds();
        assertEquals(4, firstTerminateInstanceIds.size());
        assertTrue(firstTerminateInstanceIds.contains("i-worker1"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker2"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker3"));
        assertTrue(firstTerminateInstanceIds.contains("i-worker4"));
    }

}
