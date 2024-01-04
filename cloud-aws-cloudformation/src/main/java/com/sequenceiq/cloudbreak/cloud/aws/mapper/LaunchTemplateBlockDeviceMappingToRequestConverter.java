package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;

@Component
public class LaunchTemplateBlockDeviceMappingToRequestConverter {

    @Inject
    private LaunchTemplateEbsBlockDeviceToRequestConverter ebsBlockDeviceConverter;

    public LaunchTemplateBlockDeviceMappingRequest convert(LaunchTemplateBlockDeviceMapping source) {
        LaunchTemplateBlockDeviceMappingRequest.Builder builder = LaunchTemplateBlockDeviceMappingRequest.builder()
                .deviceName(source.deviceName())
                .noDevice(source.noDevice())
                .virtualName(source.virtualName());
        if (source.ebs() != null) {
            builder.ebs(ebsBlockDeviceConverter.convert(source.ebs()));
        }
        return builder.build();
    }
}
