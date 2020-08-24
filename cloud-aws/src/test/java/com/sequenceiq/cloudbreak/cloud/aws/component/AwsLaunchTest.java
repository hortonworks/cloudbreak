package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.AVAILABILITY_ZONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.autoscaling.waiters.AmazonAutoScalingWaiters;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribePrefixListsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(SpringRunner.class)
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "cb.max.aws.resource.name.length=200",
        "cb.gcp.stopStart.batch.size=2",
        "cb.gcp.create.batch.size=2",
        "cb.aws.hostkey.verify=true",
        "cb.aws.spotinstances.enabled=true",
        "cb.aws.credential.cache.ttl=1"
})
@ActiveProfiles("component")
public class AwsLaunchTest {

    private static final int INSTANCE_STATE_RUNNING = 16;

    private static final String AUTOSCALING_GROUP_NAME = "autoscalingGroupName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String VOLUME_ID = "New Volume Id";

    private static int volumeIndex = 1;

    private static boolean describeVolumeRequestFirstInvocation = true;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private Retry retry;

    @Inject
    private ComponentTestUtil componentTestUtil;

    @MockBean
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @MockBean
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @MockBean
    private AmazonEC2Client amazonEC2Client;

    @MockBean
    private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

    @MockBean
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @MockBean
    private AmazonCloudFormationWaiters cfWaiters;

    @MockBean
    private AmazonAutoScalingWaiters asWaiters;

    @MockBean
    private AmazonEC2Waiters ecWaiters;

    @MockBean
    private Waiter<DescribeStacksRequest> cfStackWaiter;

    @MockBean
    private Waiter<DescribeInstancesRequest> instanceWaiter;

    @MockBean
    private AwsClient awsClient;

    @MockBean
    private Waiter<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestWaiter;

    @MockBean
    private Waiter<DescribeScalingActivitiesRequest> describeScalingActivitiesRequestWaiter;

    @MockBean
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Test
    public void launchStack() throws Exception {
        setup();
        setupRetryService();
        setupFreemarkerTemplateProcessing();
        setupDescribeStacksResponses();
        setupDescribeImagesResponse();
        setupDescribeStackResourceResponse();
        setupAutoscalingResponses();
        setupDescribeInstanceStatusResponse();
        setupDescribeInstancesResponse();
        setupCreateVolumeResponse();
        setupDescribeVolumeResponse();
        setupDescribeSubnetResponse();
        setupDescribePrefixListsResponse();

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);

        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        authenticatedContext.putParameter(AmazonEC2Client.class, amazonEC2Client);
        awsResourceConnector.launch(authenticatedContext, componentTestUtil.getStackForLaunch(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATE_REQUESTED),
                persistenceNotifier, AdjustmentType.EXACT, Long.MAX_VALUE);

        // assert
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VPC.equals(cloudResource.getType())), any());
        verify(persistenceNotifier, times(2))
                .notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.CLOUDFORMATION_STACK.equals(cloudResource.getType())), any());

        InOrder inOrder = inOrder(amazonEC2Client, amazonCloudFormationRetryClient);
        inOrder.verify(amazonEC2Client).describeImages(any());
        inOrder.verify(amazonCloudFormationRetryClient).createStack(any());
        inOrder.verify(amazonEC2Client, times(2)).createVolume(any());
        inOrder.verify(amazonEC2Client, times(2)).attachVolume(any());
        inOrder.verify(amazonEC2Client, never()).describePrefixLists();
    }

    private void setup() {
        when(awsClient.createAccess(any(), anyString())).thenReturn(amazonEC2Client);
        when(awsClient.createAccess(any())).thenReturn(amazonEC2Client);
        when(awsClient.createCloudFormationRetryClient(any(), anyString())).thenReturn(amazonCloudFormationRetryClient);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(cfStackWaiter);
        when(cfWaiters.stackDeleteComplete()).thenReturn(cfStackWaiter);
        when(awsClient.createAutoScalingRetryClient(any(), anyString())).thenReturn(amazonAutoScalingRetryClient);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(asWaiters.groupInService()).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(amazonEC2Client.waiters()).thenReturn(ecWaiters);
        when(ecWaiters.instanceRunning()).thenReturn(instanceWaiter);
        when(ecWaiters.instanceTerminated()).thenReturn(instanceWaiter);
        when(customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(any(), any())).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);
    }

    private void setupRetryService() {
        when(retry.testWith2SecDelayMax15Times(any())).then(answer -> ((Supplier) answer.getArgument(0)).get());
    }

    private void setupFreemarkerTemplateProcessing() throws IOException, freemarker.template.TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenReturn("processedTemplate");
    }

    private void setupCreateVolumeResponse() {
        when(amazonEC2Client.createVolume(any())).thenReturn(
                new CreateVolumeResult().withVolume(
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID + getNextVolumeId())
                )
        );
    }

    private static int getNextVolumeId() {
        return volumeIndex++;
    }

    private void setupDescribeSubnetResponse() {
        when(amazonEC2Client.describeSubnets(any())).thenAnswer(
                (Answer<DescribeSubnetsResult>) invocation -> {
                    DescribeSubnetsRequest request = (DescribeSubnetsRequest) invocation.getArgument(0);
                    String subnetId = request.getSubnetIds().get(0);
                    DescribeSubnetsResult result = new DescribeSubnetsResult()
                            .withSubnets(new com.amazonaws.services.ec2.model.Subnet()
                                    .withSubnetId(subnetId)
                                    .withAvailabilityZone(AVAILABILITY_ZONE));
                    return result;
                }
        );
    }

    private void setupDescribeVolumeResponse() {
        when(amazonEC2Client.describeVolumes(any())).thenAnswer(
                (Answer<DescribeVolumesResult>) invocation -> {
                    DescribeVolumesResult describeVolumesResult = new DescribeVolumesResult();
                    Object[] args = invocation.getArguments();
                    DescribeVolumesRequest describeVolumesRequest = (DescribeVolumesRequest) args[0];
                    VolumeState currentVolumeState = getCurrentVolumeState();
                    describeVolumesRequest.getVolumeIds().forEach(
                            volume -> describeVolumesResult.withVolumes(
                                    new com.amazonaws.services.ec2.model.Volume().withState(currentVolumeState)
                            )
                    );
                    return describeVolumesResult;
                }
        );
    }

    private VolumeState getCurrentVolumeState() {
        VolumeState currentVolumeState = describeVolumeRequestFirstInvocation ? VolumeState.Available : VolumeState.InUse;
        describeVolumeRequestFirstInvocation = false;
        return currentVolumeState;
    }

    private void setupDescribeInstanceStatusResponse() {
        when(amazonEC2Client.describeInstanceStatus(any())).thenReturn(
                new DescribeInstanceStatusResult().withInstanceStatuses(
                        new com.amazonaws.services.ec2.model.InstanceStatus().withInstanceState(new InstanceState().withCode(INSTANCE_STATE_RUNNING)))
        );
    }

    private void setupDescribeInstancesResponse() {
        when(amazonEC2Client.describeInstances(any())).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(new com.amazonaws.services.ec2.model.Instance().withInstanceId("i-instance")))
        );
    }

    private void setupDescribeStacksResponses() {
        when(amazonCloudFormationRetryClient.describeStacks(any()))
                .thenThrow(new AmazonServiceException("stack does not exist"))
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult());
    }

    private void setupDescribeImagesResponse() {
        when(amazonEC2Client.describeImages(any())).thenReturn(
                new DescribeImagesResult()
                        .withImages(new com.amazonaws.services.ec2.model.Image().withRootDeviceName(""))
        );
    }

    private void setupDescribePrefixListsResponse() {
        when(amazonEC2Client.describePrefixLists()).thenReturn(new DescribePrefixListsResult());
    }

    private void setupDescribeStackResourceResponse() {
        StackResourceDetail stackResourceDetail = new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME);
        DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult().withStackResourceDetail(stackResourceDetail);
        when(amazonCloudFormationRetryClient.describeStackResource(any())).thenReturn(describeStackResourceResult);
    }

    private void setupAutoscalingResponses() {
        DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult();
        when(amazonAutoScalingRetryClient.describeScalingActivities(any())).thenReturn(describeScalingActivitiesResult);

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(
                        new AutoScalingGroup()
                                .withInstances(new Instance().withLifecycleState(LifecycleState.InService).withInstanceId(INSTANCE_ID))
                                .withAutoScalingGroupName(AUTOSCALING_GROUP_NAME)
                );
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
    }

    private DescribeStacksResult getDescribeStacksResult() {
        return new DescribeStacksResult().withStacks(
                new Stack().withOutputs(
                        new Output().withOutputKey("CreatedVpc").withOutputValue("vpc-id"),
                        new Output().withOutputKey("CreatedSubnet").withOutputValue("subnet-id"),
                        new Output().withOutputKey("EIPAllocationIDmaster1").withOutputValue("eipalloc-id")
                ));
    }
}
