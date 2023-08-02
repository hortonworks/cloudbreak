package com.sequenceiq.cloudbreak.cloud.aws.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@ExtendWith(MockitoExtension.class)
class AwsAdditionalDiskCreatorTest {

    @Mock
    private AwsResourceNameService awsResourceNameService;

    @Mock
    private AwsCommonDiskUtilService awsCommonDiskUtilService;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @InjectMocks
    private AwsAdditionalDiskCreator underTest;

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

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void initMocksForTest() {
        doReturn("TEST").when(cloudResource).getName();
        doReturn(List.of(instance)).when(group).getInstances();
        doReturn("test-instance-id").when(instance).getInstanceId();
        doReturn(Map.of("FQDN", "test.fqdn")).when(instance).getParameters();
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn(instanceTemplate).when(group).getReferenceInstanceTemplate();
        doReturn(List.of(volume)).when(instanceTemplate).getVolumes();
        doReturn(amazonEc2Client).when(commonAwsClient).createEc2Client(authenticatedContext);
        doReturn(tagSpecification).when(awsCommonDiskUtilService).getTagSpecification(cloudStack);
        doReturn(true).when(awsCommonDiskUtilService).isEncryptedVolumeRequested(group);
        doReturn("").when(awsCommonDiskUtilService).getVolumeEncryptionKey(group, true);
    }

    @Test
    void testCreateVolumes() {
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-east-1", false, "", Lists.newArrayList(),
                200, "standard");
        volumeSetAttributes.setDiscoveryFQDN("test");
        doReturn("test-instance-id").when(cloudResource).getInstanceId();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(eq(CloudResource.ATTRIBUTES), eq(VolumeSetAttributes.class));
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume("", "", 500, "gp2", CloudVolumeUsageType.GENERAL);
        CreateVolumeResponse createResponse = mock(CreateVolumeResponse.class);
        doReturn(createResponse).when(amazonEc2Client).createVolume(any());
        doReturn("vol-1").when(createResponse).volumeId();
        List<CloudResource> response = underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack,
                2, List.of(cloudResource));
        verify(amazonEc2Client, times(2)).createVolume(any());
        CloudResource cloudResourceResponse = response.get(0);
        List<VolumeSetAttributes.Volume> responseVolumes = cloudResourceResponse.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)
                .getVolumes();
        assertEquals(2, responseVolumes.size());
        assertEquals(500, responseVolumes.get(0).getSize());
        assertEquals("gp2", responseVolumes.get(0).getType());
    }

    @Test
    void testCreateVolumesThrowsException() {
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume("", "", 500, "gp2", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-east-1", false, "", Lists.newArrayList(),
                200, "standard");
        volumeSetAttributes.setDiscoveryFQDN("test");
        doReturn("test-instance-id").when(cloudResource).getInstanceId();
        doThrow(AwsServiceException.builder().message("Test").build()).when(amazonEc2Client).createVolume(any());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(cloudResource)));
        verify(amazonEc2Client, times(1)).createVolume(any());
        assertEquals("com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException: " +
                "Error while creating and attaching disks to the instance: test-instance-id, exception: Test", exception.getMessage());
    }
}
