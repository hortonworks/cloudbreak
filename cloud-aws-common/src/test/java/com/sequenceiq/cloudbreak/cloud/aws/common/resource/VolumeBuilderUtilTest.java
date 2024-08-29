package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;

import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;

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

    @Test
    public void testGetRootVolume() {
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(ac.getCloudCredential()).thenReturn(new CloudCredential());
        software.amazon.awssdk.services.ec2.model.Image ecImage = software.amazon.awssdk.services.ec2.model.Image.builder().build();
        when(amazonEc2Client.describeImages(any())).thenReturn(DescribeImagesResponse.builder().images(ecImage).build());
        when(cloudStack.getImage()).thenReturn(image);
        BlockDeviceMapping actual = underTest.getRootVolume(awsInstanceView, group, cloudStack, ac);
        Assertions.assertNotNull(actual);
    }

    @Test
    public void testGetEphemeralWhenVolumesEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(0L);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        Assertions.assertNotNull(actual);
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void testGetEphemeralWhenVolumesNotEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(1L);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        Assertions.assertNotNull(actual);
        Assertions.assertFalse(actual.isEmpty());
        BlockDeviceMapping theSingleBlockDeviceMapping = actual.get(0);
        Assertions.assertEquals("/dev/xvdb", theSingleBlockDeviceMapping.deviceName());
        Assertions.assertEquals("ephemeral0", theSingleBlockDeviceMapping.virtualName());
    }

    @Test
    public void testGetEphemeralWhenHas25Volumes() {
        long storageCount = 25L;
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(storageCount);

        List<BlockDeviceMapping> actual = underTest.getEphemeral(awsInstanceView);

        Assertions.assertNotNull(actual);
        Assertions.assertFalse(actual.isEmpty());
        Assertions.assertEquals(storageCount, actual.size());
        Assertions.assertTrue(actual.stream()
                .anyMatch(deviceMapping -> "/dev/xvdb".equals(deviceMapping.deviceName())
                        && "ephemeral0".equals(deviceMapping.virtualName())));
        Assertions.assertTrue(actual.stream()
                .anyMatch(deviceMapping -> "/dev/xvdz".equals(deviceMapping.deviceName())
                        && "ephemeral24".equals(deviceMapping.virtualName())));
    }

    @Test
    public void testGetRootDeviceNotFoundOnAws() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any())).thenReturn(DescribeImagesResponse.builder().build());
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        Assertions.assertEquals("AMI is not available: 'null'.", actual.getMessage());
    }

    @Test
    public void testGetRootDeviceWhenImageNull() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any()))
                .thenReturn(DescribeImagesResponse.builder().images((software.amazon.awssdk.services.ec2.model.Image) null).build());
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        Assertions.assertEquals("Couldn't describe AMI 'null'.", actual.getMessage());
    }

    @Test
    public void testGetEbsWhenEncryptedAndKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(true);
        when(awsInstanceView.isKmsCustom()).thenReturn(true);
        when(awsInstanceView.getKmsKey()).thenReturn("kmsKey");

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group, null);
        Assertions.assertTrue(actual.deleteOnTermination());
        Assertions.assertTrue(actual.encrypted());
        Assertions.assertEquals("gp3", actual.volumeType().toString());
        Assertions.assertEquals(1, actual.volumeSize());
        Assertions.assertEquals("kmsKey", actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenEncryptedAndKmsKeyCustomAndVolumeType() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(group.getRootVolumeType()).thenReturn("gp2");
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(true);
        when(awsInstanceView.isKmsCustom()).thenReturn(true);
        when(awsInstanceView.getKmsKey()).thenReturn("kmsKey");

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group, null);
        Assertions.assertTrue(actual.deleteOnTermination());
        Assertions.assertTrue(actual.encrypted());
        Assertions.assertEquals("gp2", actual.volumeType().toString());
        Assertions.assertEquals(1, actual.volumeSize());
        Assertions.assertEquals("kmsKey", actual.kmsKeyId());
    }

    @Test
    public void testGetEbsWhenNotEncryptedAndNotKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(false);
        when(awsInstanceView.isKmsCustom()).thenReturn(false);

        EbsBlockDevice actual = underTest.getRootEbs(awsInstanceView, group, null);
        Assertions.assertTrue(actual.deleteOnTermination());
        Assertions.assertNull(actual.encrypted());
        Assertions.assertEquals("gp3", actual.volumeType().toString());
        Assertions.assertEquals(1, actual.volumeSize());
        Assertions.assertNull(actual.kmsKeyId());
    }
}
