package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;

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

    @Test
    public void testGetRootVolume() {
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        com.amazonaws.services.ec2.model.Image ecImage = new com.amazonaws.services.ec2.model.Image();
        when(amazonEc2Client.describeImages(any())).thenReturn(new DescribeImagesResult().withImages(ecImage));
        when(cloudStack.getImage()).thenReturn(image);
        BlockDeviceMapping actual = underTest.getRootVolume(awsInstanceView, group, cloudStack, ac);
        Assertions.assertNotNull(actual);
    }

    @Test
    public void testGetEphemeralWhenVolumesEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(0L);
        BlockDeviceMapping actual = underTest.getEphemeral(awsInstanceView);
        Assertions.assertNull(actual);
    }

    @Test
    public void testGetEphemeralWhenVolumesNotEmpty() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(1L);
        BlockDeviceMapping actual = underTest.getEphemeral(awsInstanceView);
        Assertions.assertEquals("/dev/xvdb", actual.getDeviceName());
        Assertions.assertEquals("ephemeral0", actual.getVirtualName());
    }

    @Test
    public void testGetEphemeralWhenHas25Volumes() {
        when(awsInstanceView.getTemporaryStorageCount()).thenReturn(25L);
        BlockDeviceMapping actual = underTest.getEphemeral(awsInstanceView);
        Assertions.assertEquals("/dev/xvdz", actual.getDeviceName());
        Assertions.assertEquals("ephemeral24", actual.getVirtualName());
    }

    @Test
    public void testGetRootDeviceNotFoundOnAws() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any())).thenReturn(new DescribeImagesResult().withImages());
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        Assertions.assertEquals("AMI is not available: 'null'.", actual.getMessage());
    }

    @Test
    public void testGetRootDeviceWhenImageNull() {
        when(cloudStack.getImage()).thenReturn(image);
        when(ac.getParameter(AmazonEc2Client.class)).thenReturn(amazonEc2Client);
        when(amazonEc2Client.describeImages(any())).thenReturn(new DescribeImagesResult().withImages((com.amazonaws.services.ec2.model.Image) null));
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class, () -> underTest.getRootDeviceName(ac, cloudStack));
        Assertions.assertEquals("Couldn't describe AMI 'null'.", actual.getMessage());
    }

    @Test
    public void testGetEbsWhenEncryptedAndKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(true);
        when(awsInstanceView.isKmsCustom()).thenReturn(true);
        when(awsInstanceView.getKmsKey()).thenReturn("kmsKey");

        EbsBlockDevice actual = underTest.getEbs(awsInstanceView, group);
        Assertions.assertTrue(actual.getDeleteOnTermination());
        Assertions.assertTrue(actual.getEncrypted());
        Assertions.assertEquals("gp2", actual.getVolumeType());
        Assertions.assertEquals(1, actual.getVolumeSize());
        Assertions.assertEquals("kmsKey", actual.getKmsKeyId());
    }

    @Test
    public void testGetEbsWhenNotEncryptedAndNotKmsKeyCustom() {
        when(group.getRootVolumeSize()).thenReturn(1);
        when(awsInstanceView.isEncryptedVolumes()).thenReturn(false);
        when(awsInstanceView.isKmsCustom()).thenReturn(false);

        EbsBlockDevice actual = underTest.getEbs(awsInstanceView, group);
        Assertions.assertTrue(actual.getDeleteOnTermination());
        Assertions.assertNull(actual.getEncrypted());
        Assertions.assertEquals("gp2", actual.getVolumeType());
        Assertions.assertEquals(1, actual.getVolumeSize());
        Assertions.assertNull(actual.getKmsKeyId());
    }
}
