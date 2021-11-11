package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.AVAILABILITY_ZONE;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_1;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_2;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.INSTANCE_ID_3;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.SIZE_DISK_1;
import static com.sequenceiq.cloudbreak.cloud.aws.component.ComponentTestUtil.SIZE_DISK_2;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

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
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.VolumeState;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
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
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
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
import com.sequenceiq.cloudbreak.cloud.storage.LocationHelper;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.CommonStatus;
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

    private static int efsIdIndex = 1;

    private static final String EFS_CREATIONTOKEN = "efs-creation-token";

    @MockBean
    private CreateFileSystemResult createFileSystemResult;

    @MockBean
    private DescribeFileSystemsResult describeFileSystemsResult;

    @MockBean
    private FileSystemDescription efsDescription1;

    @MockBean
    private DescribeMountTargetsResult describeMountTargetsResult;

    @MockBean
    private MountTargetDescription mtDescription;

    @MockBean
    private DeleteMountTargetResult deleteMtResult;

    @MockBean
    private DeleteFileSystemResult deleteFileSystemResult;

    @MockBean
    private LocationHelper locationHelper;

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
    private AmazonEfsClient amazonEfsClient;

    @MockBean
    private CommonAwsClient commonAwsClient;

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
    private AmazonCloudWatchClient cloudWatchClient;

    @MockBean
    private DescribeAlarmsResult describeAlarmsResult;

    @MockBean
    private Waiter<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestWaiter;

    @MockBean
    private Waiter<DescribeScalingActivitiesRequest> describeScalingActivitiesRequestWaiter;

    @MockBean
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private AwsMetadataCollector awsMetadataCollector;

    @Test
    public void repairStack() throws Exception {
        setup();
        setupRetryService();
        downscaleStack();
        Mockito.reset(amazonEC2Client, amazonEfsClient, amazonCloudFormationClient, amazonAutoScalingClient,
                persistenceNotifier);
        upscaleStack();
    }

    private void setup() {
        when(awsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        when(commonAwsClient.createEc2Client(any(), anyString())).thenReturn(amazonEC2Client);
        when(awsClient.createElasticFileSystemClient(any(), anyString())).thenReturn(amazonEfsClient);
        when(awsClient.createCloudFormationClient(any(), anyString())).thenReturn(amazonCloudFormationClient);
        when(amazonCloudFormationClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(cfStackWaiter);
        when(cfWaiters.stackDeleteComplete()).thenReturn(cfStackWaiter);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createAutoScalingClient(any(), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudWatchClient(any(), anyString())).thenReturn(cloudWatchClient);
        when(cloudWatchClient.describeAlarms(any())).thenReturn(describeAlarmsResult);
        when(describeAlarmsResult.getMetricAlarms()).thenReturn(Collections.emptyList());
        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(asWaiters.groupInService()).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(amazonEC2Client.waiters()).thenReturn(ecWaiters);
        when(ecWaiters.instanceRunning()).thenReturn(instanceWaiter);
        when(ecWaiters.instanceTerminated()).thenReturn(instanceWaiter);
        when(customAmazonWaiterProvider.getAutoscalingInstancesInServiceWaiter(any(), any())).thenReturn(describeAutoScalingGroupsRequestWaiter);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);

        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);
        when(amazonEfsClient.createFileSystem(any())).thenReturn(createFileSystemResult);
        when(createFileSystemResult.getFileSystemId()).thenReturn(EFS_FILESYSTEM_ID + efsIdIndex);
        when(createFileSystemResult.getName()).thenReturn(EFS_FILESYSTEM_ID + efsIdIndex);
        when(createFileSystemResult.getLifeCycleState()).thenReturn("creating");

        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
        when(amazonEfsClient.describeFileSystems(any())).thenReturn(describeFileSystemsResult);
        when(describeFileSystemsResult.getFileSystems()).thenReturn(Arrays.asList(efsDescription1));
        when(efsDescription1.getFileSystemId()).thenReturn(EFS_FILESYSTEM_ID + efsIdIndex);
        when(efsDescription1.getLifeCycleState()).thenReturn(LifeCycleState.Available.toString());

        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
        when(amazonEfsClient.describeMountTargets(any())).thenReturn(describeMountTargetsResult);
        when(describeMountTargetsResult.getMountTargets()).thenReturn(Arrays.asList(mtDescription));
        when(mtDescription.getMountTargetId()).thenReturn("mounttarget-1");

        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
        when(amazonEfsClient.deleteMountTarget(any())).thenReturn(deleteMtResult);
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);
        when(amazonEfsClient.deleteFileSystem(any())).thenReturn(deleteFileSystemResult);

        when(entitlementService.awsCloudStorageValidationEnabled(any())).thenReturn(Boolean.TRUE);
        when(locationHelper.parseS3BucketName(anyString())).thenCallRealMethod();
    }

    private void setupRetryService() {
        when(retry.testWith2SecDelayMax15Times(any())).then(answer -> ((Supplier) answer.getArgument(0)).get());
    }

    private void upscaleStack() throws Exception {
        AuthenticatedContext authenticatedContext = componentTestUtil.getAuthenticatedContext();
        CloudStack stack = componentTestUtil.getStack(InstanceStatus.CREATE_REQUESTED, InstanceStatus.STARTED);
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .name(AWS_SUBNET_ID)
                        .type(ResourceType.AWS_SUBNET)
                        .build(),
                createVolumeResource(VOLUME_ID_1, INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1, CommonStatus.DETACHED),
                createVolumeResource(VOLUME_ID_2, INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2, CommonStatus.DETACHED),
                createVolumeResource(VOLUME_ID_3, INSTANCE_ID_3, SIZE_DISK_2, FSTAB_2, CommonStatus.CREATED));

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);

        when(amazonCloudFormationClient.describeStackResource(any()))
                .thenReturn(new DescribeStackResourceResult()
                        .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME)));

        when(amazonAutoScalingClient.describeAutoScalingGroups(any()))
                .thenReturn(new DescribeAutoScalingGroupsResult()
                        .withAutoScalingGroups(new AutoScalingGroup()
                                .withAutoScalingGroupName(AUTOSCALING_GROUP_NAME)
                                .withInstances(List.of(
                                        new Instance().withInstanceId(INSTANCE_ID_1).withLifecycleState(LifecycleState.InService),
                                        new Instance().withInstanceId(INSTANCE_ID_2).withLifecycleState(LifecycleState.InService)))
                        ));

        when(amazonEC2Client.describeVolumes(any()))
                .thenReturn(new DescribeVolumesResult().withVolumes(
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_1).withState(VolumeState.Available),
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_2).withState(VolumeState.Available),
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_3).withState(VolumeState.InUse)
                ));

        when(amazonEC2Client.describeInstances(any())).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(new com.amazonaws.services.ec2.model.Instance().withInstanceId("i-instance")))
        );

        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        result.setActivities(List.of());
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);

        AmazonEC2Waiters waiters = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters()).thenReturn(waiters);
        Waiter<DescribeInstancesRequest> instanceWaiter = mock(Waiter.class);
        when(waiters.instanceRunning()).thenReturn(instanceWaiter);

        when(amazonAutoScalingClient.waiters()).thenReturn(asWaiters);
        when(asWaiters.groupInService()).thenReturn(describeAutoScalingGroupsRequestWaiter);

        underTest.upscale(authenticatedContext, stack, cloudResources, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 0L));

        verify(amazonAutoScalingClient).resumeProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                && argument.getScalingProcesses().contains("Launch")));
        verify(amazonAutoScalingClient).updateAutoScalingGroup(argThat(argument -> {
            Group workerGroup = stack.getGroups().get(1);
            return AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                    && workerGroup.getInstancesSize().equals(argument.getMaxSize())
                    && workerGroup.getInstancesSize().equals(argument.getDesiredCapacity());
        }));

        verify(amazonAutoScalingClient, times(stack.getGroups().size()))
                .suspendProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                        && SUSPENDED_PROCESSES.equals(argument.getScalingProcesses())));

        ArgumentCaptor<CloudResource> updatedCloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(resourceNotifier, times(2)).notifyUpdate(updatedCloudResourceArgumentCaptor.capture(), any());

        assertVolumeResource(updatedCloudResourceArgumentCaptor.getAllValues(), INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1);
        assertVolumeResource(updatedCloudResourceArgumentCaptor.getAllValues(), INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2);
    }

    private void assertVolumeResource(List<CloudResource> updatedCloudResources, String instanceId, int sizeDisk, String fstab) {
        updatedCloudResources.stream()
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
                .thenReturn(new DescribeVolumesResult().
                        withVolumes(new com.amazonaws.services.ec2.model.Volume()
                                        .withVolumeId(VOLUME_ID_1)
                                        .withState(VolumeState.InUse),
                                new com.amazonaws.services.ec2.model.Volume()
                                        .withVolumeId(VOLUME_ID_2)
                                        .withState(VolumeState.InUse)
                        ));

        when(amazonCloudFormationClient.describeStackResource(any()))
                .thenReturn(new DescribeStackResourceResult()
                        .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME)));

        when(amazonEC2Client.describeInstances(any(DescribeInstancesRequest.class))).thenAnswer(a -> {
            DescribeInstancesRequest request = a.getArgument(0, DescribeInstancesRequest.class);
            List<com.amazonaws.services.ec2.model.Instance> instances = request.getInstanceIds().stream()
                    .map(i -> new com.amazonaws.services.ec2.model.Instance().withInstanceId(i))
                    .collect(Collectors.toList());
            return new DescribeInstancesResult().withReservations(new Reservation().withInstances(instances));
        });

        AmazonEC2Waiters mockWaiter = mock(AmazonEC2Waiters.class);
        when(amazonEC2Client.waiters())
                .thenReturn(mockWaiter);
        when(mockWaiter.instanceTerminated())
                .thenReturn(mock(Waiter.class));

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setInstances(List.of(
                new Instance().withInstanceId(INSTANCE_ID_1),
                new Instance().withInstanceId(INSTANCE_ID_2),
                new Instance().withInstanceId(INSTANCE_ID_3)));
        describeAutoScalingGroupsResult.setAutoScalingGroups(List.of(autoScalingGroup));
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

        verify(amazonAutoScalingClient).detachInstances(argThat(argument -> argument.getAutoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.getShouldDecrementDesiredCapacity()
                && argument.getInstanceIds().size() == 2
                && argument.getInstanceIds().contains(INSTANCE_ID_1)
                && argument.getInstanceIds().contains(INSTANCE_ID_2)
        ));

        verify(amazonEC2Client).terminateInstances(argThat(argument -> argument.getInstanceIds().size() == 2
                && argument.getInstanceIds().contains(INSTANCE_ID_1)
                && argument.getInstanceIds().contains(INSTANCE_ID_2)));

        verify(amazonAutoScalingClient).updateAutoScalingGroup(argThat(argument -> argument.getAutoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.getMaxSize().equals(1)));
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
                .group(WORKER_GROUP)
                .name(volumeId)
                .status(status)
                .type(ResourceType.AWS_VOLUMESET)
                .instanceId(instanceId)
                .persistent(true)
                .params(attributes).build();
    }
}
