package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMappingRequest;

@Component
public class LaunchTemplateBlockDeviceMappingToRequestConverter {

    @Inject
    private LaunchTemplateEbsBlockDeviceToRequestConverter ebsBlockDeviceConverter;

    public LaunchTemplateBlockDeviceMappingRequest convert(LaunchTemplateBlockDeviceMapping source) {
        return LaunchTemplateBlockDeviceMappingRequest.builder()
                .deviceName(source.deviceName())
                .noDevice(source.noDevice())
                .virtualName(source.virtualName())
                .ebs(ebsBlockDeviceConverter.convert(source.ebs()))
                .build();
    }
}
