package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;

@ExtendWith(MockitoExtension.class)
public class VolumeBuilderUtilTest {

    @InjectMocks
    private VolumeBuilderUtil underTest;

    @Mock
    private Group group;

    @Mock
    private AwsInstanceView awsInstanceView;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private Image image;

    @Mock
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Mock
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Test
    public void testGetRootVolume() {
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        software.amazon.awssdk.services.ec2.model.Image ecImage = software.amazon.awssdk.services.ec2.model.Image.builder().build();
        when(amazonEc2Client.describeImages(any())).thenReturn(DescribeImagesResponse.builder().images(ecImage).build());
        when(cloudStack.getImage()).thenReturn(image);
        BlockDeviceMapping actual = underTest.getRootVolume(awsInstanceView, group, cloudStack, ac);
        assertNotNull(actual);
    }

    @Test
    public void testGetEphemeralWhenVolumesEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(0L);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetEphemeralWhenVolumesNotEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(1L);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        assertNotNull(actual);
        assertFalse(actual.isEmpty());
        BlockDeviceMapping theSingleBlockDeviceMapping = actual.get(0);
        assertEquals("/dev/xvdb", theSingleBlockDeviceMapping.deviceName());
        assertEquals("ephemeral0", theSingleBlockDeviceMapping.virtualName());
    }

    @Test
    public void testGetEphemeralWhenHas25Volumes() {
        long storageCount = 25L;
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(storageCount);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        assertNotNull(actual);
        assertFalse(actual.isEmpty());
        assertEquals(storageCount, actual.size());
        assertTrue(actual.stream()
                .anyMatch(deviceMapping -> "/dev/xvdb".equals(deviceMapping.deviceName())
                        && "ephemeral0".equals(deviceMapping.virtualName())));
        assertTrue(actual.stream()
                .anyMatch(deviceMapping -> "/dev/xvdz".equals(deviceMapping.deviceName())
                        && "ephemeral24".equals(deviceMapping.virtualName())));
    }

    @Test
    public void testGetRootDeviceNotFoundOnAws() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any())).thenReturn(DescribeImagesResponse.builder().build());
        CloudConnectorException actual = assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        assertEquals("AMI is not available: 'null'.", actual.getMessage());
    }

    @Test
    public void testGetRootDeviceWhenImageNull() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any()))
                .thenReturn(DescribeImagesResponse.builder().images((software.amazon.awssdk.services.ec2.model.Image) null).build());
        CloudConnectorException actual = assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        assertEquals("Couldn't describe AMI 'null'.", actual.getMessage());
    }

    @Test
    public void testGetEbsWhenEncryptedAndKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(true);
        when(awsInstanceView.isKmsCustom()).thenReturn(true);
        when(awsInstanceView.getKmsKey()).thenReturn("kmsKey");

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group);
        assertTrue(actual.deleteOnTermination());
        assertTrue(actual.encrypted());
        assertEquals("gp3", actual.volumeType().toString());
        assertEquals(1, actual.volumeSize());
        assertEquals("kmsKey", actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenEncryptedAndKmsKeyCustomAndVolumeType() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(group.getRootVolumeType()).thenReturn("gp2");
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(true);
        when(awsInstanceView.isKmsCustom()).thenReturn(true);
        when(awsInstanceView.getKmsKey()).thenReturn("kmsKey");

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group);
        assertTrue(actual.deleteOnTermination());
        assertTrue(actual.encrypted());
        assertEquals("gp2", actual.volumeType().toString());
        assertEquals(1, actual.volumeSize());
        assertEquals("kmsKey", actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenNotEncryptedAndNotKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(false);
        when(awsInstanceView.isKmsCustom()).thenReturn(false);

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group);
        assertTrue(actual.deleteOnTermination());
        assertNull(actual.encrypted());
        assertEquals("gp3", actual.volumeType().toString());
        assertEquals(1, actual.volumeSize());
        assertNull(actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenNotEncryptedAndRootVolumeTypeIsUpperCase() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(group.getRootVolumeType()).thenReturn("GP3");
        when(awsInstanceView.isKmsCustom()).thenReturn(false);

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group);
        assertTrue(actual.deleteOnTermination());
        assertNull(actual.encrypted());
        assertEquals("gp3", actual.volumeType().toString());
        assertEquals(1, actual.volumeSize());
        assertNull(actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenNotEncryptedAndRootVolumeTypeIsLowerCase() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(group.getRootVolumeType()).thenReturn("gp3");
        when(awsInstanceView.isKmsCustom()).thenReturn(false);

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group);
        assertTrue(actual.deleteOnTermination());
        assertNull(actual.encrypted());
        assertEquals("gp3", actual.volumeType().toString());
        assertEquals(1, actual.volumeSize());
        assertNull(actual.kmsKeyId());
    }

    @Test
    void testCreateRootVolumeResource() {
        CloudResource result = underTest.createRootVolumeResource("testRoot", "testGroup", ResourceType.AWS_ROOT_DISK, "1");
        assertEquals("testRoot", result.getName());
        assertEquals("testGroup", result.getGroup());
        assertEquals(ResourceType.AWS_ROOT_DISK, result.getType());
        assertEquals("1", result.getAvailabilityZone());
        assertEquals(CommonStatus.REQUESTED, result.getStatus());
    }

    @Test
    void testDescribeInstancesByInstanceIds() {
        DescribeInstancesResponse describeInstancesResponse = mock(DescribeInstancesResponse.class);
        Reservation reservation = mock(Reservation.class);
        Instance expectedInstance = mock(Instance.class);
        when(describeInstancesResponse.reservations()).thenReturn(List.of(reservation));
        when(reservation.instances()).thenReturn(List.of(expectedInstance));
        when(commonAwsClient.createEc2Client(ac)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstancesResponse);

        List<Instance> result = underTest.describeInstancesByInstanceIds(List.of("test-instance"), ac);

        assertEquals(expectedInstance, result.getFirst());
    }

    @Test
    void testGetRootVolumeIdsFromInstances() {
        Instance instance = mock(Instance.class);
        when(instance.rootDeviceName()).thenReturn("test-device-name");
        InstanceBlockDeviceMapping instanceBlockDeviceMapping = mock(InstanceBlockDeviceMapping.class);
        when(instanceBlockDeviceMapping.deviceName()).thenReturn("test-device-name");
        EbsInstanceBlockDevice ebsInstanceBlockDevice = mock(EbsInstanceBlockDevice.class);
        when(ebsInstanceBlockDevice.volumeId()).thenReturn("1");
        when(instanceBlockDeviceMapping.ebs()).thenReturn(ebsInstanceBlockDevice);
        when(instance.blockDeviceMappings()).thenReturn(List.of(instanceBlockDeviceMapping));

        List<String> result = underTest.getRootVolumeIdsFromInstances(List.of(instance));

        assertEquals("1", result.getFirst());
    }

    @Test
    void testUpdateRootVolumeResource() {
        CloudResource cloudResource = CloudResource.builder().withName("test").withType(ResourceType.AWS_ROOT_DISK)
                .withParameters(new HashMap<>()).withStatus(CommonStatus.REQUESTED).build();
        when(commonAwsClient.createEc2Client(ac)).thenReturn(amazonEc2Client);
        DescribeVolumesResponse describeVolumesResponse = mock(DescribeVolumesResponse.class);
        when(amazonEc2Client.describeVolumes(any())).thenReturn(describeVolumesResponse);
        when(describeVolumesResponse.hasVolumes()).thenReturn(true);
        software.amazon.awssdk.services.ec2.model.Volume volume = mock(software.amazon.awssdk.services.ec2.model.Volume.class);
        when(describeVolumesResponse.volumes()).thenReturn(List.of(volume));
        VolumeAttachment volumeAttachment = mock(VolumeAttachment.class);
        when(volume.attachments()).thenReturn(List.of(volumeAttachment));
        when(volumeAttachment.instanceId()).thenReturn("test-instance");
        when(volumeAttachment.device()).thenReturn("test-device");
        when(volume.volumeId()).thenReturn("test-volume-id");
        when(volume.availabilityZone()).thenReturn("test-availability-zone");
        when(volume.size()).thenReturn(100);
        when(volume.volumeTypeAsString()).thenReturn("test-volume-type");

        List<CloudResource> result = underTest.updateRootVolumeResource(List.of(cloudResource), List.of("1"), ac);
        CloudResource responseResource = result.getFirst();
        VolumeSetAttributes responseAttributes = responseResource.getParameter("attributes", VolumeSetAttributes.class);
        assertEquals("test-instance", responseResource.getInstanceId());
        assertEquals(CommonStatus.CREATED, responseResource.getStatus());
        assertEquals("test-availability-zone", responseAttributes.getAvailabilityZone());
        assertEquals("test-device", responseAttributes.getVolumes().getFirst().getDevice());
        assertEquals("test-volume-id", responseAttributes.getVolumes().getFirst().getId());
        assertEquals(100, responseAttributes.getVolumes().getFirst().getSize());
        assertEquals("test-volume-type", responseAttributes.getVolumes().getFirst().getType());
    }

    @Test
    void testUpdateRootVolumeResourceThrowsException() {
        CloudResource cloudResource = CloudResource.builder().withName("test").withType(ResourceType.AWS_ROOT_DISK)
                .withParameters(new HashMap<>()).withStatus(CommonStatus.REQUESTED).build();
        when(commonAwsClient.createEc2Client(ac)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeVolumes(any())).thenThrow(new RuntimeException("TEST"));

        List<CloudResource> result = underTest.updateRootVolumeResource(List.of(cloudResource), List.of("1"), ac);
        assertEquals(0, result.size());
    }
}
