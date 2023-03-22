package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDeviceRequest;

@ExtendWith(MockitoExtension.class)
class LaunchTemplateBlockDeviceMappingToRequestConverterTest {

    @Mock
    private LaunchTemplateEbsBlockDeviceToRequestConverter ebsBlockDeviceConverter;

    @InjectMocks
    private LaunchTemplateBlockDeviceMappingToRequestConverter underTest;

    @Test
    void testConvert() {
        LaunchTemplateBlockDeviceMapping source = LaunchTemplateBlockDeviceMapping.builder()
                .deviceName("asdf")
                .noDevice("hgtfs")
                .virtualName("hjyter")
                .ebs(mock(LaunchTemplateEbsBlockDevice.class))
                .build();
        LaunchTemplateEbsBlockDeviceRequest blockDeviceRequest = LaunchTemplateEbsBlockDeviceRequest.builder().build();
        when(ebsBlockDeviceConverter.convert(source.ebs())).thenReturn(blockDeviceRequest);

        LaunchTemplateBlockDeviceMappingRequest result = underTest.convert(source);

        assertEquals(source.deviceName(), result.deviceName());
        assertEquals(source.noDevice(), result.noDevice());
        assertEquals(source.virtualName(), result.virtualName());
        assertEquals(blockDeviceRequest, result.ebs());
    }
}