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
import java.util.Arrays;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.autoscaling.waiters.AmazonAutoScalingWaiters;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribePrefixListsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient;
import com.amazonaws.services.elasticfilesystem.model.CreateFileSystemResult;
import com.amazonaws.services.elasticfilesystem.model.DeleteFileSystemResult;
import com.amazonaws.services.elasticfilesystem.model.DeleteMountTargetResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeMountTargetsResult;
import com.amazonaws.services.elasticfilesystem.model.FileSystemDescription;
import com.amazonaws.services.elasticfilesystem.model.LifeCycleState;
import com.amazonaws.services.elasticfilesystem.model.MountTargetDescription;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
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

    private static final String EFS_FILESYSTEM_ID = "fs-";

    private static int efsIdIndex = 1;

    private static final String EFS_CREATIONTOKEN = "efs-creation-token";

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
    private LocationHelper locationHelper;

    @MockBean
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @MockBean
    private AmazonElasticFileSystemClient amazonElasticFileSystemClient;

    @MockBean
    private AmazonEfsClient amazonEfsClient;

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
    private AwsCloudFormationClient awsClient;

    @MockBean
    private CommonAwsClient commonAwsClient;

    @MockBean
    private AmazonCloudWatchClient cloudWatchClient;

    @MockBean
    private Waiter<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestWaiter;

    @MockBean
    private Waiter<DescribeScalingActivitiesRequest> describeScalingActivitiesRequestWaiter;

    @MockBean
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @MockBean
    private EntitlementService entitlementService;

    @Test
    public void launchStack() throws Exception {
        setup();
        setupRetryService();
        setupFreemarkerTemplateProcessing();
        setupDescribeStacksResponses();
        setupDescribeImagesResponse();
        setupDescribeStackResourceResponse();
        setupAutoscalingResponses();
        setupDescribeInstancesResponse();
        setupCreateVolumeResponse();
        setupDescribeVolumeResponse();
        setupDescribeSubnetResponse();
        setupDescribePrefixListsResponse();

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);

        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        authenticatedContext.putParameter(AmazonEc2Client.class, amazonEc2Client);
        awsResourceConnector.launch(authenticatedContext, componentTestUtil.getStackForLaunch(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATE_REQUESTED),
                persistenceNotifier, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, Long.MAX_VALUE));

        // assert
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VPC.equals(cloudResource.getType())), any());
        verify(persistenceNotifier, times(2))
                .notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.CLOUDFORMATION_STACK.equals(cloudResource.getType())), any());

        InOrder inOrder = inOrder(amazonEc2Client, amazonCloudFormationClient, amazonEc2Client);
        inOrder.verify(amazonEc2Client).describeImages(any());
        inOrder.verify(amazonCloudFormationClient).createStack(any());
        inOrder.verify(amazonEc2Client, times(2)).createVolume(any());
        inOrder.verify(amazonEc2Client, times(2)).attachVolume(any());
        inOrder.verify(amazonEc2Client, never()).describePrefixLists();
    }

    @Test
    public void launchStackWithEfs() throws Exception {
        setup();
        setupRetryService();
        setupFreemarkerTemplateProcessing();
        setupDescribeStacksResponses();
        setupDescribeImagesResponse();
        setupDescribeStackResourceResponse();
        setupAutoscalingResponses();
        setupDescribeInstancesResponse();
        setupCreateVolumeResponse();
        setupDescribeVolumeResponse();
        setupDescribeSubnetResponse();
        setupDescribePrefixListsResponse();

        setupCreateFileSystem();
        setupDescribeFileSystems();
        setupDescribeMountTargets();
        setupDeleteMountTarget();
        setupDeleteFileSystem();

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);

        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        authenticatedContext.putParameter(AmazonEc2Client.class, amazonEc2Client);
        authenticatedContext.putParameter(AmazonElasticFileSystemClient.class, amazonElasticFileSystemClient);
        authenticatedContext.putParameter(AmazonEfsClient.class, amazonEfsClient);
        awsResourceConnector.launch(authenticatedContext, componentTestUtil.getStackForLaunch(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATE_REQUESTED),
                persistenceNotifier, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, Long.MAX_VALUE));

        // assert
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VPC.equals(cloudResource.getType())), any());
        verify(persistenceNotifier, times(2))
                .notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.CLOUDFORMATION_STACK.equals(cloudResource.getType())), any());

        InOrder inOrder = inOrder(amazonElasticFileSystemClient, amazonEfsClient, amazonCloudFormationClient, amazonEc2Client);
        inOrder.verify(amazonEc2Client).describeImages(any());
        inOrder.verify(amazonCloudFormationClient).createStack(any());
        inOrder.verify(amazonEc2Client, times(2)).createVolume(any());
        inOrder.verify(amazonEc2Client, times(2)).attachVolume(any());
        inOrder.verify(amazonEc2Client, never()).describePrefixLists();
    }

    private void setup() {
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEc2Client);
        when(awsClient.createElasticFileSystemClient(any(), anyString())).thenReturn(amazonEfsClient);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(amazonCloudFormationClient);
        when(commonAwsClient.createEc2Client(any(), any())).thenReturn(amazonEc2Client);
        when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(cfStackWaiter);
        when(cfWaiters.stackDeleteComplete()).thenReturn(cfStackWaiter);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(asWaiters.groupInService()).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(amazonEc2Client.waiters()).thenReturn(ecWaiters);
        when(ecWaiters.instanceRunning()).thenReturn(instanceWaiter);
        when(ecWaiters.instanceTerminated()).thenReturn(instanceWaiter);
        when(customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(any(), any())).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);
        when(awsClient.createCloudWatchClient(any(), anyString())).thenReturn(cloudWatchClient);
        when(entitlementService.awsCloudStorageValidationEnabled(any())).thenReturn(Boolean.TRUE);
        when(locationHelper.parseS3BucketName(anyString())).thenCallRealMethod();
    }

    private void setupRetryService() {
        when(retry.testWith2SecDelayMax15Times(any())).then(answer -> ((Supplier) answer.getArgument(0)).get());
    }

    private void setupFreemarkerTemplateProcessing() throws IOException, freemarker.template.TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenReturn("processedTemplate");
    }

    private void setupCreateVolumeResponse() {
        when(amazonEc2Client.createVolume(any())).thenReturn(
                new CreateVolumeResult().withVolume(
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID + getNextVolumeId())
                )
        );
    }

    private static int getNextVolumeId() {
        return volumeIndex++;
    }

    private void setupDescribeSubnetResponse() {
        when(amazonEc2Client.describeSubnets(any())).thenAnswer(
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
        when(amazonEc2Client.describeVolumes(any())).thenAnswer(
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

    private void setupDescribeInstancesResponse() {
        when(amazonEc2Client.describeInstances(any())).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(new com.amazonaws.services.ec2.model.Instance().withInstanceId("i-instance")))
        );
    }

    private void setupDescribeStacksResponses() {
        when(amazonCloudFormationClient.describeStacks(any()))
                .thenThrow(new AmazonServiceException("stack does not exist"))
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult());
    }

    private void setupDescribeImagesResponse() {
        when(amazonEc2Client.describeImages(any())).thenReturn(
                new DescribeImagesResult()
                        .withImages(new com.amazonaws.services.ec2.model.Image().withRootDeviceName(""))
        );
    }

    private void setupDescribePrefixListsResponse() {
        when(amazonEc2Client.describePrefixLists()).thenReturn(new DescribePrefixListsResult());
    }

    private void setupDescribeStackResourceResponse() {
        StackResourceDetail stackResourceDetail = new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME);
        DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult().withStackResourceDetail(stackResourceDetail);
        when(amazonCloudFormationClient.describeStackResource(any())).thenReturn(describeStackResourceResult);
    }

    private void setupAutoscalingResponses() {
        DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult();
        when(amazonAutoScalingClient.describeScalingActivities(any())).thenReturn(describeScalingActivitiesResult);

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(
                        new AutoScalingGroup()
                                .withInstances(new Instance().withLifecycleState(LifecycleState.InService).withInstanceId(INSTANCE_ID))
                                .withAutoScalingGroupName(AUTOSCALING_GROUP_NAME)
                );
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
    }

    private DescribeStacksResult getDescribeStacksResult() {
        return new DescribeStacksResult().withStacks(
                new Stack().withOutputs(
                        new Output().withOutputKey("CreatedVpc").withOutputValue("vpc-id"),
                        new Output().withOutputKey("CreatedSubnet").withOutputValue("subnet-id"),
                        new Output().withOutputKey("EIPAllocationIDmaster1").withOutputValue("eipalloc-id")
                ));
    }

    private void setupCreateFileSystem() {
        String fileSystemId = EFS_FILESYSTEM_ID + efsIdIndex;
        efsIdIndex++;

        CreateFileSystemResult createFileSystemResult = new CreateFileSystemResult()
                .withFileSystemId(fileSystemId)
                .withName(fileSystemId)
                .withLifeCycleState("creating");

        when(amazonElasticFileSystemClient.createFileSystem(any())).thenReturn(createFileSystemResult);
        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);
    }

    private void setupDescribeFileSystems() {
        FileSystemDescription efsDescription1 = new FileSystemDescription().withCreationToken(EFS_CREATIONTOKEN)
                .withFileSystemId(EFS_FILESYSTEM_ID + efsIdIndex)
                .withLifeCycleState(LifeCycleState.Available.toString());

        DescribeFileSystemsResult describeFileSystemsResult = new DescribeFileSystemsResult()
                .withFileSystems(Arrays.asList(efsDescription1));

        when(amazonElasticFileSystemClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
    }

    private void setupDescribeMountTargets() {
        MountTargetDescription mtDescription = new MountTargetDescription().withMountTargetId("mounttarget-1");
        DescribeMountTargetsResult describeMountTargetsResult = new DescribeMountTargetsResult().withMountTargets(mtDescription);
        when(amazonElasticFileSystemClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
    }

    private void setupDeleteMountTarget() {
        DeleteMountTargetResult deleteMtResult = new DeleteMountTargetResult();
        when(amazonElasticFileSystemClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
    }

    private void setupDeleteFileSystem() {
        DeleteFileSystemResult deleteFileSystemResult = new DeleteFileSystemResult();
        when(amazonElasticFileSystemClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);
    }
}
