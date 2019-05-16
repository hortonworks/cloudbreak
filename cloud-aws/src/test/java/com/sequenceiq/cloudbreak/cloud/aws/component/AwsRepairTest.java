package com.sequenceiq.cloudbreak.cloud.aws.component;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.VolumeState;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.task.ASGroupStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@MockBean(ASGroupStatusCheckerTask.class)
public class AwsRepairTest extends AwsComponentTest {

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

    @Inject
    private AwsResourceConnector underTest;

    @Inject
    private AmazonEC2Client amazonEC2Client;

    @Inject
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @Inject
    private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Test
    public void repairStack() throws Exception {
        downscaleStack();
        Mockito.reset(amazonEC2Client, amazonCloudFormationRetryClient, amazonAutoScalingRetryClient, persistenceNotifier);
        upscaleStack();
    }

    private void upscaleStack() throws Exception {
        AuthenticatedContext authenticatedContext = getAuthenticatedContext();
        CloudStack stack = getStack(InstanceStatus.CREATE_REQUESTED, InstanceStatus.STARTED);
        List<CloudResource> cloudResources = List.of(
                CloudResource.builder()
                        .name(AWS_SUBNET_ID)
                        .type(ResourceType.AWS_SUBNET)
                        .build(),
                createVolumeResource(VOLUME_ID_1, null, SIZE_DISK_1, FSTAB_1),
                createVolumeResource(VOLUME_ID_2, null, SIZE_DISK_2, FSTAB_2),
                createVolumeResource(VOLUME_ID_3, INSTANCE_ID_3, SIZE_DISK_2, FSTAB_2));

        when(amazonCloudFormationRetryClient.describeStackResource(any()))
                .thenReturn(new DescribeStackResourceResult()
                        .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME)));

        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any()))
                .thenReturn(new DescribeAutoScalingGroupsResult()
                        .withAutoScalingGroups(new AutoScalingGroup()
                                .withAutoScalingGroupName(AUTOSCALING_GROUP_NAME)
                                .withInstances(List.of(
                                        new Instance().withInstanceId(INSTANCE_ID_1).withLifecycleState(LifecycleState.InService),
                                        new Instance().withInstanceId(INSTANCE_ID_2).withLifecycleState(LifecycleState.InService),
                                        new Instance().withInstanceId(INSTANCE_ID_3).withLifecycleState(LifecycleState.InService)))
                        ));

        when(amazonEC2Client.describeVolumes(any()))
                .thenReturn(new DescribeVolumesResult().withVolumes(
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_1).withState(VolumeState.Available),
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_2).withState(VolumeState.Available),
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID_3).withState(VolumeState.InUse)
                ));

        underTest.upscale(authenticatedContext, stack, cloudResources);

        verify(amazonAutoScalingRetryClient).resumeProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                && argument.getScalingProcesses().contains("Launch")));
        verify(amazonAutoScalingRetryClient).updateAutoScalingGroup(argThat(argument -> {
            Group workerGroup = stack.getGroups().get(1);
            return AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                    && workerGroup.getInstancesSize().equals(argument.getMaxSize())
                    && workerGroup.getInstancesSize().equals(argument.getDesiredCapacity());
        }));

        verify(amazonAutoScalingRetryClient, times(stack.getGroups().size()))
                .suspendProcesses(argThat(argument -> AUTOSCALING_GROUP_NAME.equals(argument.getAutoScalingGroupName())
                    && SUSPENDED_PROCESSES.equals(argument.getScalingProcesses())));

        ArgumentCaptor<CloudResource> updatedCloudResourceArgumentCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(resourceNotifier, times(4)).notifyUpdate(updatedCloudResourceArgumentCaptor.capture(), any());

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

        when(amazonCloudFormationRetryClient.describeStackResource(any()))
                .thenReturn(new DescribeStackResourceResult()
                        .withStackResourceDetail(new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME)));

        when(amazonEC2Client.describeInstances(any()))
                .thenReturn(new DescribeInstancesResult().withReservations(new Reservation().withInstances(List.of())));

        List<Volume> volumes = List.of();
        InstanceTemplate instanceTemplate = new InstanceTemplate("", WORKER_GROUP, 0L, volumes, InstanceStatus.STARTED, Map.of(), 0L, IMAGE_ID);
        InstanceAuthentication authentication = new InstanceAuthentication("publicKey", "publicKeyId", "cloudbreak");
        CloudInstance firstCloudInstance = new CloudInstance(INSTANCE_ID_1, instanceTemplate, authentication);
        CloudInstance secondCloudInstance = new CloudInstance(INSTANCE_ID_2, instanceTemplate, authentication);
        List<CloudInstance> cloudInstancesToRemove = List.of(firstCloudInstance, secondCloudInstance);

        CloudResource instance1VolumeResource = createVolumeResource(VOLUME_ID_1, INSTANCE_ID_1, SIZE_DISK_1, FSTAB_1);
        CloudResource instance2VolumeResource = createVolumeResource(VOLUME_ID_2, INSTANCE_ID_2, SIZE_DISK_2, FSTAB_2);
        List<CloudResource> resources = List.of(instance1VolumeResource, instance2VolumeResource);

        AuthenticatedContext authenticatedContext = getAuthenticatedContext();
        CloudStack cloudStack = getStack(InstanceStatus.DELETE_REQUESTED, InstanceStatus.CREATE_REQUESTED);
        underTest.downscale(authenticatedContext, cloudStack, resources, cloudInstancesToRemove, null);

        verify(persistenceNotifier).notifyUpdate(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())
                        && VOLUME_ID_1.equals(cloudResource.getName())
                        && Objects.isNull(cloudResource.getInstanceId())
                        && cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination()),
                eq(authenticatedContext.getCloudContext()));

        verify(persistenceNotifier).notifyUpdate(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())
                        && VOLUME_ID_2.equals(cloudResource.getName())
                        && Objects.isNull(cloudResource.getInstanceId())
                        && cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getDeleteOnTermination()),
                eq(authenticatedContext.getCloudContext()));

        verify(amazonAutoScalingRetryClient).detachInstances(argThat(argument -> argument.getAutoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.getShouldDecrementDesiredCapacity()
                && argument.getInstanceIds().size() == 2
                && argument.getInstanceIds().contains(INSTANCE_ID_1)
                && argument.getInstanceIds().contains(INSTANCE_ID_2)
        ));

        verify(amazonEC2Client).terminateInstances(argThat(argument -> argument.getInstanceIds().size() == 2
                && argument.getInstanceIds().contains(INSTANCE_ID_1)
                && argument.getInstanceIds().contains(INSTANCE_ID_2)));

        verify(amazonAutoScalingRetryClient).updateAutoScalingGroup(argThat(argument -> argument.getAutoScalingGroupName().equals(AUTOSCALING_GROUP_NAME)
                && argument.getMaxSize().equals(1)));
    }

    private CloudResource createVolumeResource(String volumeId, String instanceId, int sizeDisk, String fstab) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CloudResource.ATTRIBUTES, new VolumeSetAttributes.Builder()
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .withDeleteOnTermination(Boolean.FALSE)
                .withFstab(fstab)
                .withVolumes(List.of(new VolumeSetAttributes.Volume(volumeId, DEVICE, sizeDisk, VOLUME_TYPE)))
                .build());
        return CloudResource.builder()
                .group(WORKER_GROUP)
                .name(volumeId)
                .status(CommonStatus.CREATED)
                .type(ResourceType.AWS_VOLUMESET)
                .instanceId(instanceId)
                .persistent(true)
                .params(attributes).build();
    }
}
