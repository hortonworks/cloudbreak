package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.AVAILABILITY_ZONE;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_1;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_2;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_3;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.SIZE_DISK_1;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.SIZE_DISK_2;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.AutoScalingGroupHandler;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.AwsSyncUserDataService;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AsgInstanceDetachWaiter;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.polling.Poller;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.util.S3ExpressBucketNameValidator;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.autoscaling.model.LifecycleState;
import software.amazon.awssdk.services.autoscaling.waiters.AutoScalingWaiter;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResourceDetail;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;
import software.amazon.awssdk.services.efs.model.CreateFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetResponse;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
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
public class AwsRepairTest {

    private static final String WORKER_GROUP = "worker";

    private static final String VOLUME_ID_1 = "vol-0001";

    private static final String VOLUME_ID_2 = "vol-002";

    private static final String VOLUME_ID_3 = "vol-003";

    private static final String IMAGE_ID = "ami-0001";

    private static final String VOLUME_TYPE = "standard";

    private static final String FSTAB_1 = "UUID=093b3c38-8239-433a-8d99-496feb578a2e /hadoopfs/fs1 ext4 defaults,noatime,nofail 0 2";

    private static final String FSTAB_2 = "UUID=76e177a9-8195-43cf-84ae-14ea371008b6 /hadoopfs/fs1 ext4 defaults,noatime,nofail 0 2";

    private static final String DEVICE = "xvdb";

    private static final String AUTOSCALING_GROUP_NAME = "test-ag";

    private static final String AWS_SUBNET_ID = "aws-subnet";

    private static final List<String> SUSPENDED_PROCESSES = asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");

    private static final String EFS_FILESYSTEM_ID = "fs-";

    @MockBean
    private LocationHelper locationHelper;

    @MockBean
    private ResourceRetriever resourceRetriever;

    @Inject
    private AwsResourceConnector underTest;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private Retry retry;

    @Inject
    private ComponentTestUtil componentTestUtil;

    @MockBean
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @Mock
    private AmazonEc2Client amazonEC2Client;

    @MockBean
    private CloudParameterService cloudParameterService;

    @MockBean
    private AmazonEfsClient amazonEfsClient;

    @MockBean
    private CommonAwsClient commonAwsClient;

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
    private AwsMetadataCollector awsMetadataCollector;

    @MockBean
    private AwsMetricPublisher awsMetricPublisher;

    @MockBean
    private Poller<Boolean> poller;

    @MockBean
    private AwsSyncUserDataService awsSyncUserDataService;

    @MockBean
    private S3ExpressBucketNameValidator s3ExpressBucketNameValidator;

    @MockBean
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @MockBean
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Test
    public void repairStack() throws Exception {
        setup();
        setupRetryService();
        downscaleStack();
        reset(amazonEC2Client, amazonEfsClient, amazonCloudFormationClient, amazonAutoScalingClient,
                persistenceNotifier);
        upscaleStack();
    }

    private void setup() {
        doAnswer(invocation -> ((AsgInstanceDetachWaiter) invocation.getArgument(2)).process())
                .when(poller).runPoller(nullable(Long.class), nullable(Long.class), any());
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        when(commonAwsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        when(commonAwsClient.createEc2Client(isA(AuthenticatedContext.class))).thenReturn(amazonEC2Client);
        when(awsClient.createElasticFileSystemClient(any(), anyString())).thenReturn(amazonEfsClient);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudWatchClient(any(), anyString())).thenReturn(cloudWatchClient);
        when(cloudWatchClient.describeAlarms(any())).thenReturn(DescribeAlarmsResponse.builder().build());
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(amazonEC2Client.waiters()).thenReturn(ecWaiters);
        when(customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(any())).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any())).thenReturn(describeScalingActivitiesRequestWaiter);

        CreateFileSystemResponse createFileSystemResult = CreateFileSystemResponse.builder()
                .fileSystemId(EFS_FILESYSTEM_ID)
                .name(EFS_FILESYSTEM_ID)
                .lifeCycleState("creating")
                .build();
        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);
        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);

        FileSystemDescription efsDescription1 = FileSystemDescription.builder()
                .fileSystemId(EFS_FILESYSTEM_ID)
                .lifeCycleState(LifeCycleState.AVAILABLE).build();
        DescribeFileSystemsResponse describeFileSystemsResult = DescribeFileSystemsResponse.builder().fileSystems(efsDescription1).build();
        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);

        DescribeMountTargetsResponse describeMountTargetsResult = DescribeMountTargetsResponse.builder()
                .mountTargets(MountTargetDescription.builder().mountTargetId("mounttarget-1").build())
                .build();
        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);

        DeleteMountTargetResponse deleteMtResult = DeleteMountTargetResponse.builder().build();
        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
        DeleteFileSystemResponse deleteFileSystemResult = DeleteFileSystemResponse.builder().build();
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);

        when(entitlementService.awsCloudStorageValidationEnabled(any())).thenReturn(Boolean.TRUE);
        when(locationHelper.parseS3BucketName(anyString())).thenCallRealMethod();
    }

    private void setupRetryService() {
        when(retry.testWith2SecDelayMax15Times(any())).then(answer -> ((Supplier<?>) answer.getArgument(0)).get());
    }

    private void upscaleStack() throws Exception {
        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        CloudStack stack = componentTestUtil.getStack(InstanceStatus.CREATE_REQUESTED, InstanceStatus.STARTED);
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .withName(AWS_SUBNET_ID)
                        .withType(ResourceType.AWS_SUBNET)
                        .build(),
                createVolumeResource(VOLUME_ID_1, INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1, CommonStatus.DETACHED),
                createVolumeResource(VOLUME_ID_2, INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2, CommonStatus.DETACHED),
                createVolumeResource(VOLUME_ID_3, INSTANCE_ID_3, SIZE_DISK_2, FSTAB_2, CommonStatus.CREATED));

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);

        when(amazonCloudFormationClient.describeStackResource(any()))
                .thenReturn(DescribeStackResourceResponse.builder()
                        .stackResourceDetail(StackResourceDetail.builder().physicalResourceId(AUTOSCALING_GROUP_NAME).build()).build());

        when(amazonAutoScalingClient.describeAutoScalingGroups(any()))
                .thenReturn(DescribeAutoScalingGroupsResponse.builder()
                        .autoScalingGroups(AutoScalingGroup.builder()
                                .autoScalingGroupName(AUTOSCALING_GROUP_NAME)
                                .instances(List.of(
                                        Instance.builder().instanceId(INSTANCE_ID_1).lifecycleState(LifecycleState.IN_SERVICE).build(),
                                        Instance.builder().instanceId(INSTANCE_ID_2).lifecycleState(LifecycleState.IN_SERVICE).build()))
                                .build()
                        ).build());

        when(amazonEC2Client.describeVolumes(any()))
                .thenReturn(DescribeVolumesResponse.builder().volumes(
                        software.amazon.awssdk.services.ec2.model.Volume.builder().volumeId(VOLUME_ID_1).state(VolumeState.AVAILABLE).build(),
                        software.amazon.awssdk.services.ec2.model.Volume.builder().volumeId(VOLUME_ID_2).state(VolumeState.AVAILABLE).build(),
                        software.amazon.awssdk.services.ec2.model.Volume.builder().volumeId(VOLUME_ID_3).state(VolumeState.IN_USE).build()
                ).build());

        when(amazonEC2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(software.amazon.awssdk.services.ec2.model.Instance.builder().instanceId("i-instance").build())
                        .build())
                .build()
        );

        DescribeScalingActivitiesResponse result = DescribeScalingActivitiesResponse.builder().activities(List.of()).build();
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);

        when(amazonEC2Client.waiters()).thenReturn(ecWaiters);
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);

        underTest.upscale(authenticatedContext, stack, cloudResources, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L));

        verify(amazonAutoScalingClient).resumeProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.autoScalingGroupName())
                && argument.scalingProcesses().contains("Launch")));
        verify(amazonAutoScalingClient).updateAutoScalingGroup(argThat(argument -> {
            Group workerGroup = stack.getGroups().get(1);
            return AUTOSCALING_GROUP_NAME.equals(argument.autoScalingGroupName())
                    && workerGroup.getInstancesSize().equals(argument.maxSize())
                    && workerGroup.getInstancesSize().equals(argument.desiredCapacity());
        }));

        verify(amazonAutoScalingClient, times(stack.getGroups().size()))
                .suspendProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.autoScalingGroupName())
                        && SUSPENDED_PROCESSES.equals(argument.scalingProcesses())));

        ArgumentCaptor<List<CloudResource>> updatedCloudResourceArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(resourceNotifier, times(6)).notifyUpdates(updatedCloudResourceArgumentCaptor.capture(), any());

        assertVolumeResource(updatedCloudResourceArgumentCaptor.getAllValues(), INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1);
        assertVolumeResource(updatedCloudResourceArgumentCaptor.getAllValues(), INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2);
    }

    private void assertVolumeResource(List<List<CloudResource>> updatedCloudResources, String instanceId, int sizeDisk, String fstab) {
        updatedCloudResources.stream()
                .flatMap(Collection::stream)
                .filter(cloudResource -> instanceId.equals(cloudResource.getInstanceId()))
                .findFirst()
                .ifPresentOrElse(cloudResource -> {
                    VolumeSetAttributes volumeSetAttributes = cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
                    assertTrue(cloudResource.isPersistent());
                    assertEquals(instanceId, cloudResource.getInstanceId());
                    assertEquals(ResourceType.AWS_VOLUMESET, cloudResource.getType());
                    assertEquals(WORKER_GROUP, cloudResource.getGroup());
                    assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
                    assertEquals(AVAILABILITY_ZONE, volumeSetAttributes.getAvailabilityZone());
                    assertEquals(Integer.valueOf(sizeDisk), volumeSetAttributes.getVolumes().get(0).getSize());
                    assertEquals("standard", volumeSetAttributes.getVolumes().get(0).getType());
                    assertEquals(fstab, volumeSetAttributes.getFstab());
                }, () -> fail("Volume resource was not saved for " + instanceId));
    }

    private void downscaleStack() throws IOException {
        when(amazonEC2Client.describeVolumes(any()))
                .thenReturn(DescribeVolumesResponse.builder()
                        .volumes(software.amazon.awssdk.services.ec2.model.Volume.builder()
                                        .volumeId(VOLUME_ID_1)
                                        .state(VolumeState.IN_USE).build(),
                                software.amazon.awssdk.services.ec2.model.Volume.builder()
                                        .volumeId(VOLUME_ID_2)
                                        .state(VolumeState.IN_USE).build()
                        ).build());

        when(amazonCloudFormationClient.describeStackResource(any()))
                .thenReturn(DescribeStackResourceResponse.builder()
                        .stackResourceDetail(StackResourceDetail.builder().physicalResourceId(AUTOSCALING_GROUP_NAME).build()).build());

        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenAnswer(a -> {
            DescribeInstancesRequest request = a.getArgument(0, DescribeInstancesRequest.class);
            List<software.amazon.awssdk.services.ec2.model.Instance> instances = request.instanceIds().stream()
                    .map(i -> software.amazon.awssdk.services.ec2.model.Instance.builder().instanceId(i).build())
                    .collect(Collectors.toList());
            return DescribeInstancesResponse.builder().reservations(Reservation.builder().instances(instances).build()).build();
        });

        when(amazonEC2Client.waiters()).thenReturn(ecWaiters);

        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder()
                        .instances(
                                Instance.builder().instanceId(INSTANCE_ID_1).build(),
                                Instance.builder().instanceId(INSTANCE_ID_2).build(),
                                Instance.builder().instanceId(INSTANCE_ID_3).build())
                        .build())
                .build();
        when(amazonAutoScalingClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);

        List<Volume> volumes = List.of();
        InstanceTemplate instanceTemplate = new InstanceTemplate("", WORKER_GROUP, 0L, volumes, InstanceStatus.STARTED, Map.of(), 0L,
                IMAGE_ID, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        InstanceAuthentication authentication = new InstanceAuthentication("publicKey", "publicKeyId", "cloudbreak");
        CloudInstance firstCloudInstance = new CloudInstance(INSTANCE_ID_1, instanceTemplate, authentication, "subnet-1", "az1");
        CloudInstance secondCloudInstance = new CloudInstance(INSTANCE_ID_2, instanceTemplate, authentication, "subnet-1", "az1");
        List<CloudInstance> cloudInstancesToRemove = List.of(firstCloudInstance, secondCloudInstance);

        CloudResource instance1VolumeResource = createVolumeResource(VOLUME_ID_1, INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1, CommonStatus.CREATED);
        CloudResource instance2VolumeResource = createVolumeResource(VOLUME_ID_2, INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2, CommonStatus.CREATED);
        List<CloudResource> resources = List.of(instance1VolumeResource, instance2VolumeResource);

        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        CloudStack cloudStack = componentTestUtil.getStack(InstanceStatus.DELETE_REQUESTED, InstanceStatus.CREATE_REQUESTED);
        underTest.downscale(authenticatedContext, cloudStack, resources, cloudInstancesToRemove, null);

        verify(persistenceNotifier).notifyUpdate(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())
                        && VOLUME_ID_1.equals(cloudResource.getName())
                        && INSTANCE_ID_1.equals(cloudResource.getInstanceId())
                        && CommonStatus.DETACHED.equals(cloudResource.getStatus())
                        && cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination()),
                eq(authenticatedContext.getCloudContext()));

        verify(persistenceNotifier).notifyUpdate(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())
                        && VOLUME_ID_2.equals(cloudResource.getName())
                        && INSTANCE_ID_2.equals(cloudResource.getInstanceId())
                        && CommonStatus.DETACHED.equals(cloudResource.getStatus())
                        && cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination()),
                eq(authenticatedContext.getCloudContext()));

        verify(amazonAutoScalingClient).detachInstances(argThat(argument -> argument.autoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.shouldDecrementDesiredCapacity()
                && argument.instanceIds().size() == 2
                && argument.instanceIds().contains(INSTANCE_ID_1)
                && argument.instanceIds().contains(INSTANCE_ID_2)
        ));

        verify(amazonEC2Client).terminateInstances(argThat(argument -> argument.instanceIds().size() == 2
                && argument.instanceIds().contains(INSTANCE_ID_1)
                && argument.instanceIds().contains(INSTANCE_ID_2)));

        verify(amazonAutoScalingClient).updateAutoScalingGroup(argThat(argument -> argument.autoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.maxSize().equals(1)));
    }

    private CloudResource createVolumeResource(String volumeId, String instanceId, int sizeDisk, String fstab, CommonStatus status) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .withDeleteOnTermination(Boolean.FALSE)
                .withFstab(fstab)
                .withVolumes(List.of(new VolumeSetAttributes.Volume(volumeId, DEVICE, sizeDisk, VOLUME_TYPE, CloudVolumeUsageType.GENERAL)))
                .build());
        return CloudResource.builder()
                .withGroup(WORKER_GROUP)
                .withName(volumeId)
                .withStatus(status)
                .withType(ResourceType.AWS_VOLUMESET)
                .withInstanceId(instanceId)
                .withPersistent(true)
                .withParameters(attributes).build();
    }
}
