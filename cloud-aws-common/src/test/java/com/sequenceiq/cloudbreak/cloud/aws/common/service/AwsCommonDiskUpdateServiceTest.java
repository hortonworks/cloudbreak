package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeThroughputCalculator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@ExtendWith(MockitoExtension.class)
public class AwsCommonDiskUpdateServiceTest {

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsVolumeIopsCalculator awsVolumeIopsCalculator;

    @Mock
    private AwsVolumeThroughputCalculator awsVolumeThroughputCalculator;

    @InjectMocks
    @Spy
    private AwsCommonDiskUpdateService underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private TagSpecification tagSpecification;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private Group group;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private CloudInstance instance;

    @BeforeEach
    public void setUp() {
        doReturn("TEST").when(cloudResource).getName();
        doReturn(List.of(instance)).when(group).getInstances();
        doReturn("us-east-1").when(instance).getAvailabilityZone();
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        Volume volume = mock(Volume.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        doReturn(List.of(volume)).when(instanceTemplate).getVolumes();
        doReturn(amazonEc2Client).when(underTest).getEc2Client(authenticatedContext);
        doReturn(tagSpecification).when(underTest).getTagSpecification(cloudStack);
        doReturn(true).when(underTest).isEncryptedVolumeRequested(group);
        doReturn("").when(underTest).getVolumeEncryptionKey(group, true);
    }

    @Test
    void testCreateAndAttachVolumes() {
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-east-1", false, "", Lists.newArrayList(),
                200, "standard");
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(eq(CloudResource.ATTRIBUTES), eq(VolumeSetAttributes.class));
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume("", "", 500, "gp2", CloudVolumeUsageType.GENERAL);
        CreateVolumeResponse createResponse = mock(CreateVolumeResponse.class);
        doReturn(createResponse).when(amazonEc2Client).createVolume(any());
        doReturn("vol-1").when(createResponse).volumeId();
        List<CloudResource> response = underTest.createAndAttachVolumes(authenticatedContext, group, volumeRequest, cloudStack,
                2, List.of(cloudResource));
        verify(amazonEc2Client, times(2)).createVolume(any());
        verify(amazonEc2Client, times(2)).attachVolume(any());
        CloudResource cloudResourceResponse = response.get(0);
        List<VolumeSetAttributes.Volume> responseVolumes = cloudResourceResponse.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)
                .getVolumes();
        Assertions.assertEquals(2, responseVolumes.size());
        Assertions.assertEquals(500, responseVolumes.get(0).getSize());
        Assertions.assertEquals("gp2", responseVolumes.get(0).getType());
    }

    @Test
    void testCreateAndAttachVolumesThrowsException() {
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume("", "", 500, "gp2", CloudVolumeUsageType.GENERAL);
        CreateVolumeResponse createResponse = mock(CreateVolumeResponse.class);
        doThrow(AwsServiceException.builder().message("Test").build()).when(amazonEc2Client).createVolume(any());

        CloudbreakServiceException exception = Assertions.assertThrows(CloudbreakServiceException.class,
                () -> underTest.createAndAttachVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(cloudResource)));
        verify(amazonEc2Client, times(1)).createVolume(any());
        verify(amazonEc2Client, times(0)).attachVolume(any());
        Assertions.assertEquals("software.amazon.awssdk.awscore.exception.AwsServiceException: Test", exception.getMessage());
    }
}
