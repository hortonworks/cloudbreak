package com.sequenceiq.cloudbreak.cloud.aws.encryption;

import static com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService.VOLUME_NOT_FOUND_MSG_CODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

public class EncryptedSnapshotServiceTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String SNAPSHOT_NOT_FOUND_MSG_CODE = "InvalidSnapshot.NotFound";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmazonEC2Client ec2Client;

    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private EncryptedSnapshotService underTest;

    public AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "crn", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential("crn", null);
        return new AuthenticatedContext(cloudContext, credential);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authenticatedContext = authenticatedContext();
    }

    @Test
    public void deleteResources() {
        String encryptedVolumeId = "vol-12345678";
        String encryptedSnapshotId = "snap-12345678";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                .name(encryptedVolumeId)
                .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                .build(),
                new CloudResource.Builder()
                .name(encryptedSnapshotId)
                .type(ResourceType.AWS_SNAPSHOT)
                .build());

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesWhenMultipleVolumeAndSnapshotExist() {
        String encryptedVolumeId = "vol-12345678";
        String encryptedVolumeId2 = "vol-123456789";
        String encryptedSnapshotId = "snap-12345678";
        String encryptedSnapshotId2 = "snap-12345679";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedVolumeId2)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId2)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(2)).deleteSnapshot(any());
        verify(ec2Client, times(2)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesShouldNotThrowExceptionWhenVolumeDoesNotExistAtDeletion() {
        String encryptedVolumeId = "vol-12345678";
        String encryptedSnapshotId = "snap-12345678";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        String eMsg = String.format("An error occurred (%s) when calling the DeleteVolume operation: The volume '%s' does not exist.",
                VOLUME_NOT_FOUND_MSG_CODE, encryptedVolumeId);
        when(ec2Client.deleteVolume(any())).thenThrow(new AmazonServiceException(eMsg));

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesShouldNotThrowExceptionWhenSnapshotDoesNotExistAtDeletion() {
        String encryptedVolumeId = "vol-12345678";
        String encryptedSnapshotId = "snap-12345678";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        String eMsg = String.format("An error occurred (%s) when calling the DeleteSnapshot operation: None", SNAPSHOT_NOT_FOUND_MSG_CODE);
        when(ec2Client.deleteSnapshot(any())).thenThrow(new AmazonServiceException(eMsg));

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesShouldNotThrowExceptionWhenSnapshotAndVolumeDoNotExistAtDeletion() {
        String encryptedVolumeId = "vol-12345678";
        String encryptedSnapshotId = "snap-12345678";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        String eMsg = String.format("An error occurred (%s) when calling the DeleteSnapshot operation: None", SNAPSHOT_NOT_FOUND_MSG_CODE);
        when(ec2Client.deleteSnapshot(any())).thenThrow(new AmazonServiceException(eMsg));
        String eMsg2 = String.format("An error occurred (%s) when calling the DeleteVolume operation: The volume '%s' does not exist.",
                VOLUME_NOT_FOUND_MSG_CODE, encryptedVolumeId);
        when(ec2Client.deleteVolume(any())).thenThrow(new AmazonServiceException(eMsg2));

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesShouldNotTryToDeleteVolumesAndThrowExceptionWhenSnapshotDeletionFails() {
        thrown.expect(CloudConnectorException.class);

        String encryptedVolumeId = "vol-12345678";
        String encryptedVolumeId2 = "vol-123456789";
        String encryptedSnapshotId = "snap-12345678";
        String encryptedSnapshotId2 = "snap-12345679";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedVolumeId2)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId2)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        when(ec2Client.deleteSnapshot(any()))
                .thenThrow(new AmazonServiceException("Something went wrong or your credentials has been expired"));

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(1)).deleteSnapshot(any());
        verify(ec2Client, times(0)).deleteVolume(any());
    }

    @Test
    public void deleteResourcesShouldThrowExceptionWhenVolumeDeletionFails() {
        thrown.expect(CloudConnectorException.class);

        String encryptedVolumeId = "vol-12345678";
        String encryptedVolumeId2 = "vol-123456789";
        String encryptedSnapshotId = "snap-12345678";
        String encryptedSnapshotId2 = "snap-12345679";

        List<CloudResource> resources = List.of(
                new CloudResource.Builder()
                        .name(encryptedVolumeId)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedVolumeId2)
                        .type(ResourceType.AWS_ENCRYPTED_VOLUME)
                        .build(),
                new CloudResource.Builder()
                        .name(encryptedSnapshotId2)
                        .type(ResourceType.AWS_SNAPSHOT)
                        .build());

        when(ec2Client.deleteSnapshot(any()))
                .thenThrow(new AmazonServiceException("Something went wrong or your credentials has been expired"));

        underTest.deleteResources(authenticatedContext, ec2Client, resources);

        verify(ec2Client, times(2)).deleteSnapshot(any());
        verify(ec2Client, times(1)).deleteVolume(any());
    }
}