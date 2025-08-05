package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.AVAILABILITY_ZONE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.util.S3ExpressBucketNameValidator;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;
import software.amazon.awssdk.services.autoscaling.waiters.AutoScalingWaiter;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceDetail;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribePrefixListsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.CreateFileSystemRequest;
import software.amazon.awssdk.services.efs.model.CreateFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemRequest;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetRequest;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetResponse;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsRequest;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsResponse;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;
import software.amazon.awssdk.services.efs.model.LifeCycleState;
import software.amazon.awssdk.services.efs.model.MountTargetDescription;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "cb.max.aws.resource.name.length=200",
        "cb.aws.hostkey.verify=true",
        "cb.aws.spotinstances.enabled=true",
        "cb.aws.credential.cache.ttl=1",
        "cb.db.override.aws.fallback.enabled=true",
        "cb.db.override.aws.fallback.targetversion=11.16",
        "aws.s3express-name-pattern=--x-s3"
})
@ActiveProfiles("component")
public class AwsLaunchTest {

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
    private CloudParameterService cloudParameterService;

    @MockBean
    private EfsClient amazonElasticFileSystemClient;

    @MockBean
    private AmazonEfsClient amazonEfsClient;

    @MockBean
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @MockBean
    private CloudFormationWaiter cfWaiters;

    @MockBean
    private AutoScalingWaiter asWaiters;

    @MockBean
    private Ec2Waiter ecWaiters;

    @MockBean
    private AwsCloudFormationClient awsClient;

    @MockBean
    private CommonAwsClient commonAwsClient;

    @MockBean
    private AmazonCloudWatchClient cloudWatchClient;

    @MockBean
    private Waiter<DescribeAutoScalingGroupsResponse> describeAutoScalingGroupsRequestWaiter;

    @MockBean
    private Waiter<DescribeScalingActivitiesResponse> describeScalingActivitiesRequestWaiter;

    @MockBean
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private ResourceRetriever resourceRetriever;

    @MockBean
    private AwsMetricPublisher awsMetricPublisher;

    @MockBean
    private S3ExpressBucketNameValidator s3ExpressBucketNameValidator;

    @Inject
    private ResourceBuilders resourceBuilders;

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
        verify(persistenceNotifier, times(2)).notifyAllocations(
                argThat(cloudResources -> cloudResources.stream().allMatch(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType()))),
                any());
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
        authenticatedContext.putParameter(EfsClient.class, amazonElasticFileSystemClient);
        authenticatedContext.putParameter(AmazonEfsClient.class, amazonEfsClient);
        awsResourceConnector.launch(authenticatedContext, componentTestUtil.getStackForLaunch(InstanceStatus.CREATE_REQUESTED, InstanceStatus.CREATE_REQUESTED),
                persistenceNotifier, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, Long.MAX_VALUE));

        // assert
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VPC.equals(cloudResource.getType())), any());
        verify(persistenceNotifier, times(2)).notifyAllocations(
                argThat(cloudResources -> cloudResources.stream().allMatch(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType()))),
                any());
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
        when(commonAwsClient.createEc2Client(isA(AuthenticatedContext.class))).thenReturn(amazonEc2Client);
        when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(amazonEc2Client.waiters()).thenReturn(ecWaiters);
        when(customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(any())).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any())).thenReturn(describeScalingActivitiesRequestWaiter);
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
        when(amazonEc2Client.createVolume(any())).thenReturn(CreateVolumeResponse.builder().volumeId(VOLUME_ID + getNextVolumeId()).build());
    }

    private static int getNextVolumeId() {
        return volumeIndex++;
    }

    private void setupDescribeSubnetResponse() {
        when(amazonEc2Client.describeSubnets(any())).thenAnswer(
                (Answer<DescribeSubnetsResponse>) invocation -> {
                    DescribeSubnetsRequest request = invocation.getArgument(0);
                    String subnetId = request.subnetIds().get(0);
                    return DescribeSubnetsResponse.builder()
                            .subnets(Subnet.builder()
                                    .subnetId(subnetId)
                                    .availabilityZone(AVAILABILITY_ZONE)
                                    .build())
                            .build();
                }
        );
    }

    private void setupDescribeVolumeResponse() {
        when(amazonEc2Client.describeVolumes(any())).thenAnswer(
                (Answer<DescribeVolumesResponse>) invocation -> {
                    DescribeVolumesResponse.Builder describeVolumesResult = DescribeVolumesResponse.builder();
                    Object[] args = invocation.getArguments();
                    DescribeVolumesRequest describeVolumesRequest = (DescribeVolumesRequest) args[0];
                    VolumeState currentVolumeState = getCurrentVolumeState();
                    describeVolumesRequest.volumeIds().forEach(
                            volume -> describeVolumesResult.volumes(Volume.builder().state(currentVolumeState).build())
                    );
                    return describeVolumesResult.build();
                }
        );
    }

    private VolumeState getCurrentVolumeState() {
        VolumeState currentVolumeState = describeVolumeRequestFirstInvocation ? VolumeState.AVAILABLE : VolumeState.IN_USE;
        describeVolumeRequestFirstInvocation = false;
        return currentVolumeState;
    }

    private void setupDescribeInstancesResponse() {
        when(amazonEc2Client.describeInstances(any())).thenReturn(
                DescribeInstancesResponse.builder()
                        .reservations(Reservation.builder()
                                .instances(software.amazon.awssdk.services.ec2.model.Instance.builder().instanceId("i-instance").build())
                                .build())
                        .build()
        );
    }

    private void setupDescribeStacksResponses() {
        when(amazonCloudFormationClient.describeStacks(any()))
                .thenThrow(AwsServiceException.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("stack does not exist").build()).build())
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult());
    }

    private void setupDescribeImagesResponse() {
        when(amazonEc2Client.describeImages(any())).thenReturn(DescribeImagesResponse.builder().images(Image.builder().rootDeviceName("").build()).build());
    }

    private void setupDescribePrefixListsResponse() {
        when(amazonEc2Client.describePrefixLists()).thenReturn(DescribePrefixListsResponse.builder().build());
    }

    private void setupDescribeStackResourceResponse() {
        StackResourceDetail stackResourceDetail = StackResourceDetail.builder().physicalResourceId(AUTOSCALING_GROUP_NAME).build();
        DescribeStackResourceResponse describeStackResourceResult = DescribeStackResourceResponse.builder().stackResourceDetail(stackResourceDetail).build();
        when(amazonCloudFormationClient.describeStackResource(any())).thenReturn(describeStackResourceResult);
    }

    private void setupAutoscalingResponses() {
        DescribeScalingActivitiesResponse describeScalingActivitiesResult = DescribeScalingActivitiesResponse.builder().build();
        when(amazonAutoScalingClient.describeScalingActivities(any())).thenReturn(describeScalingActivitiesResult);

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(
                        AutoScalingGroup.builder()
                                .instances(Instance.builder().lifecycleState(LifecycleState.IN_SERVICE).instanceId(INSTANCE_ID).build())
                                .autoScalingGroupName(AUTOSCALING_GROUP_NAME)
                                .build()
                ).build();
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
    }

    private DescribeStacksResponse getDescribeStacksResult() {
        return DescribeStacksResponse.builder().stacks(
                Stack.builder().outputs(
                        Output.builder().outputKey("CreatedVpc").outputValue("vpc-id").build(),
                        Output.builder().outputKey("CreatedSubnet").outputValue("subnet-id").build(),
                        Output.builder().outputKey("EIPAllocationIDmaster1").outputValue("eipalloc-id").build()
                ).build()).build();
    }

    private void setupCreateFileSystem() {
        String fileSystemId = EFS_FILESYSTEM_ID + efsIdIndex;
        efsIdIndex++;

        CreateFileSystemResponse createFileSystemResult = CreateFileSystemResponse.builder()
                .fileSystemId(fileSystemId)
                .name(fileSystemId)
                .lifeCycleState("creating")
                .build();

        when(amazonElasticFileSystemClient.createFileSystem(any(CreateFileSystemRequest.class))).thenReturn(createFileSystemResult);
        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);
    }

    private void setupDescribeFileSystems() {
        FileSystemDescription efsDescription1 = FileSystemDescription.builder()
                .creationToken(EFS_CREATIONTOKEN)
                .fileSystemId(EFS_FILESYSTEM_ID + efsIdIndex)
                .lifeCycleState(LifeCycleState.AVAILABLE)
                .build();

        DescribeFileSystemsResponse describeFileSystemsResult = DescribeFileSystemsResponse.builder()
                .fileSystems(efsDescription1)
                .build();

        when(amazonElasticFileSystemClient.describeFileSystems(any(DescribeFileSystemsRequest.class))).thenReturn(describeFileSystemsResult);
        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
    }

    private void setupDescribeMountTargets() {
        MountTargetDescription mtDescription = MountTargetDescription.builder().mountTargetId("mounttarget-1").build();
        DescribeMountTargetsResponse describeMountTargetsResult = DescribeMountTargetsResponse.builder().mountTargets(mtDescription).build();
        when(amazonElasticFileSystemClient.describeMountTargets(any(DescribeMountTargetsRequest.class))).thenReturn(describeMountTargetsResult);
        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
    }

    private void setupDeleteMountTarget() {
        DeleteMountTargetResponse deleteMtResult = DeleteMountTargetResponse.builder().build();
        when(amazonElasticFileSystemClient.deleteMountTarget(any(DeleteMountTargetRequest.class))).thenReturn(deleteMtResult);
        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
    }

    private void setupDeleteFileSystem() {
        DeleteFileSystemResponse deleteFileSystemResult = DeleteFileSystemResponse.builder().build();
        when(amazonElasticFileSystemClient.deleteFileSystem(any(DeleteFileSystemRequest.class))).thenReturn(deleteFileSystemResult);
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);
    }
}
