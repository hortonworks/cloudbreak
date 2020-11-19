package com.sequenceiq.cloudbreak.cloud.aws.encryption;

import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService.AMI_NOT_FOUND_MSG_CODE;
import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService.SNAPSHOT_NOT_FOUND_MSG_CODE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExpectedExceptionSupport;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CopyImageResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.waiters.AmazonEC2Waiters;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
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
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(ExpectedExceptionSupport.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private AmazonEC2Waiters ec2Waiters;

    @Mock
    private Waiter<DescribeImagesRequest> waiter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CloudStack cloudStack;

    @Mock
    private PersistenceNotifier resourceNotifier;

    @InjectMocks
    private EncryptedImageCopyService underTest;

    private Optional<CloudFileSystemView> identity = Optional.empty();

    @BeforeEach
    public void setUp() {
        when(cloudStack.getImage().getImageName()).thenReturn("ami-12345678");
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
    }

    @Test
    public void testCreateEncryptedImagesWhenNoEncryptionIsRequired() {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 0L, "imageId");
        testCreateEncryptedImagesWhenNoImagesAreExpected(temp);
    }

    private void testCreateEncryptedImagesWhenNoImagesAreExpected(InstanceTemplate temp) {
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30, identity));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30, identity));
        when(cloudStack.getGroups()).thenReturn(groups);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        assertThat(encryptedImages).isEmpty();
    }

    @Test
    public void testCreateEncryptedImagesWhenFastEncryptionIsRequired() {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true, AwsInstanceTemplate.FAST_EBS_ENCRYPTION_ENABLED, true), 0L, "imageId");
        testCreateEncryptedImagesWhenNoImagesAreExpected(temp);
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEncryptedWithTheDefaultKmsKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30, identity));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30, identity));
        when(cloudStack.getGroups()).thenReturn(groups);

        String encryptedImageId = "ami-87654321";
        when(ec2Client.copyImage(any())).thenReturn(new CopyImageResult().withImageId(encryptedImageId));

        when(ec2Client.waiters()).thenReturn(ec2Waiters);
        when(ec2Waiters.imageAvailable()).thenReturn(waiter);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(1)).copyImage(any());
        verify(resourceNotifier, times(1)).notifyAllocation(any(), any());
        verify(waiter, times(1)).run(any());
        assertThat(encryptedImages.size()).isEqualTo(2);
        assertThat(encryptedImages.values().stream().allMatch(el -> el.equals(encryptedImageId))).isTrue();
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEncryptedWithCustomKmsKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        InstanceTemplate temp = new InstanceTemplate("medium", "groupName", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true,
                        InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30, identity));
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance), null, null, null, null, null, null, 30, identity));
        when(cloudStack.getGroups()).thenReturn(groups);

        String encryptedImageId = "ami-87654321";
        when(ec2Client.copyImage(any())).thenReturn(new CopyImageResult().withImageId(encryptedImageId));

        when(ec2Client.waiters()).thenReturn(ec2Waiters);
        when(ec2Waiters.imageAvailable()).thenReturn(waiter);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(1)).copyImage(any());
        verify(resourceNotifier, times(1)).notifyAllocation(any(), any());
        verify(waiter, times(1)).run(any());
        assertThat(encryptedImages.size()).isEqualTo(2);
        assertThat(encryptedImages.values().stream().allMatch(el -> el.equals(encryptedImageId))).isTrue();
    }

    @Test
    public void testCreateEncryptedImagesWhenEveryGroupNeedToBeEncryptedWithDifferentKmsKeys()
            throws InterruptedException, ExecutionException, TimeoutException {
        String encryptedImageId = "ami-87654321";
        String secondEncryptedImageId = "ami-87652222";
        List<Group> groups = new ArrayList<>();

        InstanceTemplate temp = new InstanceTemplate("medium", "master", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true,
                        InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30, identity));

        InstanceTemplate temp2 = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true,
                        InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "arn:aws:kms:eu-west-1:980678888888:key/almafa23-6ac8"), 1L, "imageId");
        CloudInstance instance2 = new CloudInstance("SECOND_ID", temp2, null);
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance2), null, null, null, null, null, null, 30, identity));

        when(cloudStack.getGroups()).thenReturn(groups);

        when(ec2Client.copyImage(any()))
                .thenReturn(new CopyImageResult().withImageId(encryptedImageId))
                .thenReturn(new CopyImageResult().withImageId(secondEncryptedImageId));

        when(ec2Client.waiters()).thenReturn(ec2Waiters);
        when(ec2Waiters.imageAvailable()).thenReturn(waiter);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(2)).copyImage(any());
        verify(resourceNotifier, times(2)).notifyAllocation(any(), any());
        verify(waiter, times(1)).run(any());
        assertThat(encryptedImages.size()).isEqualTo(2);
        assertThat(encryptedImages).containsValue(encryptedImageId);
        assertThat(encryptedImages).containsValue(secondEncryptedImageId);
    }

    @Test
    public void testCreateEncryptedImagesWhenThereAreGroupsWithDefaultAndCustomKmsKeysAndWithoutEncryptionRequired()
            throws InterruptedException, ExecutionException, TimeoutException {
        String encryptedImageId = "ami-87654321";
        String secondEncryptedImageId = "ami-87652222";
        List<Group> groups = new ArrayList<>();

        InstanceTemplate temp = new InstanceTemplate("medium", "master", 0L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true,
                        InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "arn:aws:kms:eu-west-1:980678888888:key/7e9173f2-6ac8"), 0L, "imageId");
        CloudInstance instance = new CloudInstance("SOME_ID", temp, null);
        groups.add(new Group("master", InstanceGroupType.GATEWAY, singletonList(instance), null, null, null, null, null, null, 30, identity));

        InstanceTemplate temp2 = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, true,
                        InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "arn:aws:kms:eu-west-1:980678888888:key/almafa23-6ac8"), 1L, "imageId");
        CloudInstance instance2 = new CloudInstance("SECOND_ID", temp2, null);
        groups.add(new Group("worker", InstanceGroupType.CORE, singletonList(instance2), null, null, null, null, null, null, 30, identity));

        InstanceTemplate unencryptedTemp = new InstanceTemplate("medium", "worker", 1L, emptyList(), InstanceStatus.CREATE_REQUESTED,
                Map.of(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, false), 1L, "imageId");
        CloudInstance unencryptedInstance = new CloudInstance("UNENCRYPTED_ID", unencryptedTemp, null);
        groups.add(new Group("compute", InstanceGroupType.CORE, singletonList(unencryptedInstance), null, null, null, null, null, null, 30, identity));

        when(cloudStack.getGroups()).thenReturn(groups);

        when(ec2Client.copyImage(any()))
                .thenReturn(new CopyImageResult().withImageId(encryptedImageId))
                .thenReturn(new CopyImageResult().withImageId(secondEncryptedImageId));

        when(ec2Client.waiters()).thenReturn(ec2Waiters);
        when(ec2Waiters.imageAvailable()).thenReturn(waiter);

        Map<String, String> encryptedImages = underTest.createEncryptedImages(authenticatedContext(), cloudStack, resourceNotifier);

        verify(ec2Client, times(2)).copyImage(any());
        verify(resourceNotifier, times(2)).notifyAllocation(any(), any());
        verify(waiter, times(1)).run(any());
        assertThat(encryptedImages.size()).isEqualTo(2);
        assertThat(encryptedImages).containsValue(encryptedImageId);
        assertThat(encryptedImages).containsValue(secondEncryptedImageId);
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
        CloudContext cloudContext = new CloudContext(5L, "name", "crn", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential("crn", null);
        return new AuthenticatedContext(cloudContext, credential);
    }
}
