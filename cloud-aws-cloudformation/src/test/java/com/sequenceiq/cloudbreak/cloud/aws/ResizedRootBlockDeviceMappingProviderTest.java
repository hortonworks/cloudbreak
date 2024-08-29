package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.LaunchTemplateBlockDeviceMappingToRequestConverter;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

import software.amazon.awssdk.services.autoscaling.model.BlockDeviceMapping;
import software.amazon.awssdk.services.autoscaling.model.Ebs;
import software.amazon.awssdk.services.autoscaling.model.LaunchConfiguration;
import software.amazon.awssdk.services.autoscaling.model.LaunchTemplateSpecification;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
import software.amazon.awssdk.services.ec2.model.ResponseLaunchTemplateData;
import software.amazon.awssdk.services.ec2.model.VolumeType;

@ExtendWith(MockitoExtension.class)
class ResizedRootBlockDeviceMappingProviderTest {

    private static final String ROOT_DEV_NAME = "/dev/dba";

    private static final String NEW_ROOT_DEV_NAME = "/dev/diff";

    private static final String OLD_AMI = "ami-1234-old";

    private static final String NEW_AMI = "ami-5678-new";

    @Mock
    private VolumeBuilderUtil volumeBuilderUtil;

    @Mock
    private LaunchTemplateBlockDeviceMappingToRequestConverter blockDeviceMappingConverter;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private Image image;

    @Mock
    private AuthenticatedContext ac;

    @InjectMocks
    private ResizedRootBlockDeviceMappingProvider underTest;

    @Test
    void testCreateResizedRootBlockDeviceMappingNoRootDiskInFields() {
        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(), LaunchTemplateSpecification.builder().build(), cloudStack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateFails() {
        when(ec2Client.describeLaunchTemplateVersions(any(DescribeLaunchTemplateVersionsRequest.class))).thenThrow(Ec2Exception.create("asdf", null));

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().build(), cloudStack);

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateEmpty() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder().build());

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertTrue(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateNoDefault() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder().defaultVersion(Boolean.FALSE).build())
                .build());

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertTrue(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateWithRootDiskNameDiff() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        ArgumentCaptor<LaunchTemplateBlockDeviceMapping> blockDeviceMappingArgumentCaptor = ArgumentCaptor.forClass(LaunchTemplateBlockDeviceMapping.class);
        when(blockDeviceMappingConverter.convert(blockDeviceMappingArgumentCaptor.capture()))
                .thenReturn(LaunchTemplateBlockDeviceMappingRequest.builder().build());
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .imageId(OLD_AMI)
                                .blockDeviceMappings(LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName("dummy")
                                                .build(),
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName(ROOT_DEV_NAME)
                                                .ebs(LaunchTemplateEbsBlockDevice.builder().volumeSize(100).build())
                                                .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(NEW_ROOT_DEV_NAME);
        when(volumeBuilderUtil.getRootDeviceName(OLD_AMI, ec2Client)).thenReturn(ROOT_DEV_NAME);
        Map<LaunchTemplateField, String> updatableField = new HashMap<>();
        updatableField.put(LaunchTemplateField.ROOT_DISK_PATH, "");
        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, updatableField,
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        List<LaunchTemplateBlockDeviceMapping> launchTemplateBlockDeviceMappings = blockDeviceMappingArgumentCaptor.getAllValues();
        assertFalse(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
        assertEquals(launchTemplateBlockDeviceMappings.get(1).deviceName(), NEW_ROOT_DEV_NAME);
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateWithRootDiskNameDiffAndSizeDiff() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        ArgumentCaptor<LaunchTemplateBlockDeviceMapping> blockDeviceMappingArgumentCaptor = ArgumentCaptor.forClass(LaunchTemplateBlockDeviceMapping.class);
        when(blockDeviceMappingConverter.convert(blockDeviceMappingArgumentCaptor.capture()))
                .thenReturn(LaunchTemplateBlockDeviceMappingRequest.builder().build());
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .imageId(OLD_AMI)
                                .blockDeviceMappings(LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName("dummy")
                                                .build(),
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName(ROOT_DEV_NAME)
                                                .ebs(LaunchTemplateEbsBlockDevice.builder().volumeSize(100).build())
                                                .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(NEW_ROOT_DEV_NAME);
        when(volumeBuilderUtil.getRootDeviceName(OLD_AMI, ec2Client)).thenReturn(ROOT_DEV_NAME);
        Map<LaunchTemplateField, String> updatableField = new HashMap<>();
        updatableField.put(LaunchTemplateField.ROOT_DISK_PATH, "");
        updatableField.put(LaunchTemplateField.ROOT_DISK_SIZE, "150");
        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, updatableField,
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        List<LaunchTemplateBlockDeviceMapping> launchTemplateBlockDeviceMappings = blockDeviceMappingArgumentCaptor.getAllValues();
        assertFalse(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
        assertEquals(launchTemplateBlockDeviceMappings.get(1).deviceName(), NEW_ROOT_DEV_NAME);
        assertEquals(launchTemplateBlockDeviceMappings.get(1).ebs().volumeSize(), 150);
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateEmptyBlockDeviceMapping() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder().build())
                        .build())
                .build());
        mockCloudstackImage(ROOT_DEV_NAME);

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertTrue(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateNoRootDevice() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .blockDeviceMappings(LaunchTemplateBlockDeviceMapping.builder()
                                        .deviceName("dummy")
                                        .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(ROOT_DEV_NAME);
        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertTrue(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
    }

    private void mockCloudstackImage(String rootDeviceName) {
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn(NEW_AMI);
        when(volumeBuilderUtil.getRootDeviceName(NEW_AMI, ec2Client)).thenReturn(rootDeviceName);
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateRequestedAndCurrentSizeEqual() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .blockDeviceMappings(LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName("dummy")
                                                .build(),
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName(ROOT_DEV_NAME)
                                                .ebs(LaunchTemplateEbsBlockDevice.builder().volumeSize(100).build())
                                                .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(ROOT_DEV_NAME);

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertTrue(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateResizeRequired() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .blockDeviceMappings(
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName("dummy")
                                                .build(),
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName(ROOT_DEV_NAME)
                                                .ebs(LaunchTemplateEbsBlockDevice.builder().volumeSize(50).build())
                                                .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(ROOT_DEV_NAME);

        ArgumentCaptor<LaunchTemplateBlockDeviceMapping> blockDeviceMappingArgumentCaptor = ArgumentCaptor.forClass(LaunchTemplateBlockDeviceMapping.class);
        when(blockDeviceMappingConverter.convert(blockDeviceMappingArgumentCaptor.capture()))
                .thenReturn(LaunchTemplateBlockDeviceMappingRequest.builder().build());

        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        assertEquals(2, result.size());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
        List<LaunchTemplateBlockDeviceMapping> launchTemplateBlockDeviceMappings = blockDeviceMappingArgumentCaptor.getAllValues();
        assertEquals(2, launchTemplateBlockDeviceMappings.size());
        assertTrue(launchTemplateBlockDeviceMappings.stream()
                .filter(blockDeviceMapping -> blockDeviceMapping.deviceName().equals(ROOT_DEV_NAME))
                .allMatch(blockDeviceMapping -> blockDeviceMapping.ebs().volumeSize().equals(100)));
    }

    @Test
    void testCreateBlockDeviceMappingIfRootDiskResizeRequiredLaunchConfNoBlockDevice() {
        Optional<List<BlockDeviceMapping>> result = underTest.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack,
                Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"), LaunchConfiguration.builder().blockDeviceMappings(List.of()).build());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateBlockDeviceMappingIfRootDiskResizeRequiredLaunchConfNoRootDiskInUpdatableFields() {
        Optional<List<BlockDeviceMapping>> result = underTest.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack,
                Map.of(), LaunchConfiguration.builder().blockDeviceMappings(BlockDeviceMapping.builder().build()).build());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateBlockDeviceMappingIfRootDiskResizeRequiredLaunchConfNoRootDeviceMapping() {
        when(volumeBuilderUtil.getRootDeviceName(ac, cloudStack)).thenReturn(ROOT_DEV_NAME);

        Optional<List<BlockDeviceMapping>> result = underTest.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack,
                Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                LaunchConfiguration.builder().blockDeviceMappings(BlockDeviceMapping.builder()
                                .deviceName("dummy")
                                .build()).
                        build());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateBlockDeviceMappingIfRootDiskResizeRequiredLaunchConfRootDeviceSizeIsUpToDate() {
        when(volumeBuilderUtil.getRootDeviceName(ac, cloudStack)).thenReturn(ROOT_DEV_NAME);

        Optional<List<BlockDeviceMapping>> result = underTest.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack,
                Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                LaunchConfiguration.builder().blockDeviceMappings(
                                BlockDeviceMapping.builder()
                                        .deviceName("dummy")
                                        .build(),
                                BlockDeviceMapping.builder()
                                        .deviceName(ROOT_DEV_NAME)
                                        .ebs(Ebs.builder().volumeSize(100).build())
                                        .build())
                        .build());

        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateBlockDeviceMappingIfRootDiskResizeRequiredLaunchConfRootDeviceResized() {
        when(volumeBuilderUtil.getRootDeviceName(ac, cloudStack)).thenReturn(ROOT_DEV_NAME);

        Optional<List<BlockDeviceMapping>> result = underTest.createBlockDeviceMappingIfRootDiskResizeRequired(ac, cloudStack,
                Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "100"),
                LaunchConfiguration.builder().blockDeviceMappings(
                                BlockDeviceMapping.builder()
                                        .deviceName("dummy")
                                        .build(),
                                BlockDeviceMapping.builder()
                                        .deviceName(ROOT_DEV_NAME)
                                        .ebs(Ebs.builder().volumeSize(50).build())
                                        .build())
                        .build());

        assertTrue(result.isPresent());
        List<BlockDeviceMapping> blockDeviceMappings = result.get();
        assertEquals(100,
                blockDeviceMappings.stream()
                        .filter(blockDeviceMapping -> ROOT_DEV_NAME.equals(blockDeviceMapping.deviceName()))
                        .findFirst().get()
                        .ebs().volumeSize());
        assertEquals(2, blockDeviceMappings.size());
        assertTrue(blockDeviceMappings.stream().anyMatch(blockDeviceMapping -> "dummy".equals(blockDeviceMapping.deviceName())));
    }

    @Test
    void testCreateResizedRootBlockDeviceMappingDescribeLaunchTemplateWithRootDiskNameDiffAndTypeDiff() {
        ArgumentCaptor<DescribeLaunchTemplateVersionsRequest> requestCaptor = ArgumentCaptor.forClass(DescribeLaunchTemplateVersionsRequest.class);
        ArgumentCaptor<LaunchTemplateBlockDeviceMapping> blockDeviceMappingArgumentCaptor = ArgumentCaptor.forClass(LaunchTemplateBlockDeviceMapping.class);
        when(blockDeviceMappingConverter.convert(any(LaunchTemplateBlockDeviceMapping.class)))
                .thenReturn(LaunchTemplateBlockDeviceMappingRequest.builder().build());
        when(ec2Client.describeLaunchTemplateVersions(requestCaptor.capture())).thenReturn(DescribeLaunchTemplateVersionsResponse.builder()
                .launchTemplateVersions(LaunchTemplateVersion.builder()
                        .defaultVersion(Boolean.TRUE)
                        .launchTemplateData(ResponseLaunchTemplateData.builder()
                                .imageId(OLD_AMI)
                                .blockDeviceMappings(LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName("dummy")
                                                .build(),
                                        LaunchTemplateBlockDeviceMapping.builder()
                                                .deviceName(ROOT_DEV_NAME)
                                                .ebs(LaunchTemplateEbsBlockDevice.builder().volumeSize(100).volumeType(VolumeType.GP3).build())
                                                .build())
                                .build())
                        .build())
                .build());
        mockCloudstackImage(NEW_ROOT_DEV_NAME);
        when(volumeBuilderUtil.getRootDeviceName(OLD_AMI, ec2Client)).thenReturn(ROOT_DEV_NAME);
        Map<LaunchTemplateField, String> updatableField = new HashMap<>();
        updatableField.put(LaunchTemplateField.ROOT_DISK_PATH, "");
        updatableField.put(LaunchTemplateField.ROOT_DISK_SIZE, "150");
        updatableField.put(LaunchTemplateField.ROOT_VOLUME_TYPE, "gp2");
        List<LaunchTemplateBlockDeviceMappingRequest> result =
                underTest.createUpdatedRootBlockDeviceMapping(ec2Client, updatableField,
                        LaunchTemplateSpecification.builder().launchTemplateId("templateId").build(), cloudStack);

        verify(blockDeviceMappingConverter, times(2)).convert(blockDeviceMappingArgumentCaptor.capture());
        List<LaunchTemplateBlockDeviceMapping> launchTemplateBlockDeviceMappings = blockDeviceMappingArgumentCaptor.getAllValues();
        assertFalse(result.isEmpty());
        assertEquals("templateId", requestCaptor.getValue().launchTemplateId());
        assertEquals(launchTemplateBlockDeviceMappings.get(1).deviceName(), NEW_ROOT_DEV_NAME);
        assertEquals(launchTemplateBlockDeviceMappings.get(1).ebs().volumeSize(), 150);
        assertEquals(launchTemplateBlockDeviceMappings.get(1).ebs().volumeType(), VolumeType.GP2);
    }
}