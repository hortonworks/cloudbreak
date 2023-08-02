package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;

import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@ExtendWith(MockitoExtension.class)
class AwsCommonDiskUtilServiceTest {

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Mock
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    @InjectMocks
    private AwsCommonDiskUtilService underTest;

    @Test
    void testIsEncryptedVolumeRequested() {
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        doReturn(true).when(instanceTemplate).getParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Object.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        assertTrue(underTest.isEncryptedVolumeRequested(group));
    }

    @Test
    void testIsEncryptedVolumeRequestedNull() {
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        doReturn(null).when(instanceTemplate).getParameter(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED, Object.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        assertFalse(underTest.isEncryptedVolumeRequested(group));
    }

    @Test
    void testVolumeEncryptionKey() {
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        doReturn("CUSTOM").when(instanceTemplate).getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE);
        doReturn("TEST_KEY").when(instanceTemplate).getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        assertEquals("TEST_KEY", underTest.getVolumeEncryptionKey(group, true));
    }

    @Test
    void testVolumeEncryptionKeyNull() {
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        assertNull(underTest.getVolumeEncryptionKey(group, true));
    }

    @Test
    void testVolumeEncryptionKeyWhenVolumeNotEncrypted() {
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        assertNull(underTest.getVolumeEncryptionKey(group, false));
    }

    @Test
    void testTagSpecification() {
        CloudStack cloudStack = mock(CloudStack.class);
        Map<String, String> stackTags = Map.of("test", "value");
        doReturn(stackTags).when(cloudStack).getTags();
        List<Tag> tags = new ArrayList<>();
        Tag tag = software.amazon.awssdk.services.ec2.model.Tag.builder().key("test").value("value").build();
        tags.add(tag);
        doCallRealMethod().when(awsTaggingService).prepareEc2Tags(eq(stackTags));
        assertEquals(tags, underTest.getTagSpecification(cloudStack).tags());
        verify(awsTaggingService).prepareEc2Tags(eq(stackTags));
    }

    @Test
    void testTagSpecificationEmptyStackTags() {
        CloudStack cloudStack = mock(CloudStack.class);
        doReturn(Map.of()).when(cloudStack).getTags();
        doCallRealMethod().when(awsTaggingService).prepareEc2Tags(any());
        assertEquals(List.of(), underTest.getTagSpecification(cloudStack).tags());
        verify(awsTaggingService).prepareEc2Tags(any());
    }

    @Test
    void testCreateVolumeRequest() {
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        TagSpecification tagSpecification = mock(TagSpecification.class);
        doReturn("test").when(volume).getType();
        doReturn(1).when(volume).getSize();
        CreateVolumeRequest createVolumeRequest = underTest.createVolumeRequest(volume, tagSpecification, "volumeEncryptionKey", true, "az");
        verify(awsVolumeIopsCalculator).getIops(eq("test"), eq(1));
        verify(awsVolumeThroughputCalculator).getThroughput(eq("test"), eq(1));
        assertEquals(1, createVolumeRequest.size());
        assertEquals("az", createVolumeRequest.availabilityZone());
    }
}