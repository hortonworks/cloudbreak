package com.sequenceiq.cloudbreak.cloud.aws.encryption;

import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService.AMI_NOT_FOUND_MSG_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService.SNAPSHOT_NOT_FOUND_MSG_CODE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CopyImageResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Image;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.task.AMICopyStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public class EncryptedImageCopyServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String DEFAULT_REGION = "DefaultRegion";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AwsClient awsClient;

    @Mock
    private AmazonEC2Client ec2Client;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudStack cloudStack;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @Mock
    private AMICopyStatusCheckerTask amiCopyStatusCheckerTask;

    @InjectMocks
    private EncryptedImageCopyService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(cloudStack.getImage().getImageName()).thenReturn("ami-12345678");
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
    }

    @Test
    public void testCreateEncryptedImagesWhenNoEncryptionIsRequired() {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", false), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30));
        when(cloudStack.getGroups()).thenReturn(groups);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        Assert.assertTrue(encryptedImages.isEmpty());
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEncryptedWithTheDefaultKmsKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30));
        when(cloudStack.getGroups()).thenReturn(groups);

        String encryptedImageId = "ami-87654321";
        when(ec2Client.copyImage(any())).thenReturn(new CopyImageResult().withImageId(encryptedImageId));

        when(awsPollTaskFactory.newAMICopyStatusCheckerTask(any(), any(), any())).thenReturn(amiCopyStatusCheckerTask);
        when(amiCopyStatusCheckerTask.call()).thenReturn(false);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(1)).copyImage(any());
        verify(resourceNotifier, times(1)).notifyAllocation(any(), any());
        verify(syncPollingScheduler, times(1)).schedule(amiCopyStatusCheckerTask);
        Assert.assertEquals(encryptedImages.size(), 2);
        Assert.assertTrue(encryptedImages.values().stream().allMatch(el -> el.equals(encryptedImageId)));
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEncryptedWithCustomKmsKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true, "key", "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30));
        when(cloudStack.getGroups()).thenReturn(groups);

        String encryptedImageId = "ami-87654321";
        when(ec2Client.copyImage(any())).thenReturn(new CopyImageResult().withImageId(encryptedImageId));

        when(awsPollTaskFactory.newAMICopyStatusCheckerTask(any(), any(), any())).thenReturn(amiCopyStatusCheckerTask);
        when(amiCopyStatusCheckerTask.call()).thenReturn(false);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(1)).copyImage(any());
        verify(resourceNotifier, times(1)).notifyAllocation(any(), any());
        verify(syncPollingScheduler, times(1)).schedule(amiCopyStatusCheckerTask);
        Assert.assertEquals(encryptedImages.size(), 2);
        Assert.assertTrue(encryptedImages.values().stream().allMatch(el -> el.equals(encryptedImageId)));
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEnryptedWithDifferentKmsKeys()
            throws InterruptedException, ExecutionException, TimeoutException {
        String encryptedImageId = "ami-87654321";
        String secondEncryptedImageId = "ami-87652222";
        List<Group> groups = new ArrayList<>();

        InstanceTemplate temp = new InstanceTemplate("medium", "master", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true, "key", "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30));

        InstanceTemplate temp2 = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true, "key", "arn:aws:kms:eu-west-1:980678888888:key/almafa23-6ac8"), 1L, "imageId");
        CloudInstance instance2 = new CloudInstance("SECOND_ID", temp2, null);
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance2), null, null, null, null, null, null, 30));

        when(cloudStack.getGroups()).thenReturn(groups);

        when(ec2Client.copyImage(any()))
                .thenReturn(new CopyImageResult().withImageId(encryptedImageId))
                .thenReturn(new CopyImageResult().withImageId(secondEncryptedImageId));

        when(awsPollTaskFactory.newAMICopyStatusCheckerTask(any(), any(), any())).thenReturn(amiCopyStatusCheckerTask);
        when(amiCopyStatusCheckerTask.call()).thenReturn(false);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(2)).copyImage(any());
        verify(resourceNotifier, times(2)).notifyAllocation(any(), any());
        verify(syncPollingScheduler, times(1)).schedule(amiCopyStatusCheckerTask);
        Assert.assertEquals(encryptedImages.size(), 2);
        Assert.assertTrue(encryptedImages.containsValue(encryptedImageId));
        Assert.assertTrue(encryptedImages.containsValue(secondEncryptedImageId));
    }

    @Test
    public void testCreateEncryptedImagesWhenThereAreGroupsWithDefaultAndCustomKmsKeysAndWithoutEncryptionRequired()
            throws InterruptedException, ExecutionException, TimeoutException {
        String encryptedImageId = "ami-87654321";
        String secondEncryptedImageId = "ami-87652222";
        List<Group> groups = new ArrayList<>();

        InstanceTemplate temp = new InstanceTemplate("medium", "master", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true, "key", "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30));

        InstanceTemplate temp2 = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", true, "key", "arn:aws:kms:eu-west-1:980678888888:key/almafa23-6ac8"), 1L, "imageId");
        CloudInstance instance2 = new CloudInstance("SECOND_ID", temp2, null);
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance2), null, null, null, null, null, null, 30));

        InstanceTemplate unencryptedTemp = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of("encrypted", false), 1L, "imageId");
        CloudInstance unencryptedInstance = new CloudInstance("UNENCRYPTED_ID", unencryptedTemp, null);
        groups.add(new Group("compute", InstanceGroupType.CORE, singletonList(unencryptedInstance), null, null, null, null, null, null, 30));

        when(cloudStack.getGroups()).thenReturn(groups);

        when(ec2Client.copyImage(any()))
                .thenReturn(new CopyImageResult().withImageId(encryptedImageId))
                .thenReturn(new CopyImageResult().withImageId(secondEncryptedImageId));

        when(awsPollTaskFactory.newAMICopyStatusCheckerTask(any(), any(), any())).thenReturn(amiCopyStatusCheckerTask);
        when(amiCopyStatusCheckerTask.call()).thenReturn(false);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(2)).copyImage(any());
        verify(resourceNotifier, times(2)).notifyAllocation(any(), any());
        verify(syncPollingScheduler, times(1)).schedule(amiCopyStatusCheckerTask);
        Assert.assertEquals(encryptedImages.size(), 2);
        Assert.assertTrue(encryptedImages.containsValue(encryptedImageId));
        Assert.assertTrue(encryptedImages.containsValue(secondEncryptedImageId));
    }

    @Test
    public void testDeleteResourcesWhenBothAMIAndSnapshotExist() {
        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(
                        new BlockDeviceMapping().withDeviceName("/dev/sdb").withVirtualName("ephemeral0"),
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));

        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deregisterImage(any());
    }

    @Test
    public void testDeleteResourcesWhenOnlyAMIExistsAndSnapshotNot() {
        String encryptedImageId = "ami-87654321";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(new BlockDeviceMapping()
                        .withEbs(new EbsBlockDevice().withEncrypted(true)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(0)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deregisterImage(any());
    }

    @Test
    public void testDeleteResourcesWhenAMIDoesNotExistShouldNotThrowException() {
        String encryptedImageId = "ami-87654321";
        String eMsg = String.format("An error occurred (%s) when calling the DescribeImages operation: The image id '[%s]' does not exist",
                AMI_NOT_FOUND_MSG_CODE, encryptedImageId);
        when(ec2Client.describeImages(any()))
                .thenThrow(new AmazonServiceException(eMsg));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(0)).deleteSnapshot(any());
        verify(ec2Client, times(0)).deregisterImage(any());
    }

    @Test
    public void testDeleteResourcesWhenAMIDoesNotExistAtDeregisterShouldNotThrowException() {
        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        String eMsg = String.format("An error occurred (%s) when calling the DeregisterImage operation: The image id '[%s]' does not exist",
                AMI_NOT_FOUND_MSG_CODE, encryptedImageId);

        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(new BlockDeviceMapping()
                        .withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));
        when(ec2Client.deregisterImage(any()))
                .thenThrow(new AmazonServiceException(eMsg));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deregisterImage(any());
        verify(ec2Client, times(0)).deleteSnapshot(any());
    }

    @Test
    public void testDeleteResourcesWhenSnapshotDoesNotExistShouldNotThrowException() {
        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        String eMsg = String.format("An error occurred (%s) when calling the DeleteSnapshot operation: None", SNAPSHOT_NOT_FOUND_MSG_CODE);
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(new BlockDeviceMapping()
                        .withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));
        when(ec2Client.deleteSnapshot(any()))
                .thenThrow(new AmazonServiceException(eMsg));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deregisterImage(any());
        verify(ec2Client, times(1)).deleteSnapshot(any());
    }

    @Test
    public void testDeleteResourcesWhenAMIDescribeFailsWithEC2Client() {
        thrown.expect(CloudConnectorException.class);

        String encryptedImageId = "ami-87654321";
        when(ec2Client.describeImages(any()))
                .thenThrow(new AmazonServiceException("Something went wrong or your credentials has been expired"));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(0)).deleteSnapshot(any());
        verify(ec2Client, times(0)).deregisterImage(any());
    }

    @Test
    public void testDeleteResourcesWhenAMIDeregisterFailsWithEC2Client() {
        thrown.expect(CloudConnectorException.class);

        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(
                        new BlockDeviceMapping().withDeviceName("/dev/sdb").withVirtualName("ephemeral0"),
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));
        when(ec2Client.deregisterImage(any()))
                .thenThrow(new AmazonServiceException("Something went wrong or your credentials has been expired"));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(0)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deregisterImage(any());
    }

    @Test
    public void testDeleteResourcesWhenAMISnapshotDeletionFailsWithEC2Client() {
        thrown.expect(CloudConnectorException.class);

        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(new BlockDeviceMapping()
                        .withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));
        when(ec2Client.deleteSnapshot(any()))
                .thenThrow(new AmazonServiceException("Something went wrong or your credentials has been expired"));
        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deregisterImage(any());
        verify(ec2Client, times(1)).deleteSnapshot(any());
    }

    @Test
    public void testDeleteResourcesShouldDeleteMultipleSnapshotsWhenMultipleSnapshotAreLinkedToTheAMI() {
        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        String secondEncryptedSnapshotId = "snap-12345555";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(
                        new BlockDeviceMapping().withDeviceName("/dev/sdb").withVirtualName("ephemeral0"),
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)),
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(secondEncryptedSnapshotId)));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));

        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deregisterImage(any());
        verify(ec2Client, times(2)).deleteSnapshot(any());
    }

    @Test
    public void testDeleteResourcesShouldDeleteOnlyEbsDeviceMappingsWithSnapshotWhenMultipleSnapshotAndEphemeralDevicesAreLinkedToTheAMI() {
        String encryptedImageId = "ami-87654321";
        String encryptedSnapshotId = "snap-12345678";
        String secondEncryptedSnapshotId = "snap-12345555";
        Image image = new Image()
                .withImageId(encryptedImageId)
                .withBlockDeviceMappings(
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(encryptedSnapshotId)),
                        new BlockDeviceMapping().withEbs(new EbsBlockDevice().withEncrypted(true).withSnapshotId(secondEncryptedSnapshotId)),
                        new BlockDeviceMapping().withDeviceName("/dev/sdb").withVirtualName("ephemeral0"),
                        new BlockDeviceMapping().withDeviceName("/dev/sdc").withVirtualName("ephemeral1"));
        when(ec2Client.describeImages(any()))
                .thenReturn(new DescribeImagesResult().withImages(image));

        List<CloudResource> resources = List.of(new CloudResource.Builder()
                .name(encryptedImageId)
                .type(ResourceType.AWS_ENCRYPTED_AMI)
                .build());

        underTest.deleteResources(DEFAULT_REGION, ec2Client, resources);

        verify(ec2Client, times(1)).deregisterImage(any());
        verify(ec2Client, times(2)).deleteSnapshot(any());
    }

    protected static AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential(1L, null);
        return new AuthenticatedContext(cloudContext, credential);
    }
}