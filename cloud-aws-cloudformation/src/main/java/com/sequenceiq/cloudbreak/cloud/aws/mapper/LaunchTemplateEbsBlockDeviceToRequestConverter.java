package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDeviceRequest;

@Component
public class LaunchTemplateEbsBlockDeviceToRequestConverter {

    public LaunchTemplateEbsBlockDeviceRequest convert(LaunchTemplateEbsBlockDevice source) {
        return LaunchTemplateEbsBlockDeviceRequest.builder()
                .deleteOnTermination(source.deleteOnTermination())
                .volumeSize(source.volumeSize())
                .encrypted(source.encrypted())
                .iops(source.iops())
                .kmsKeyId(source.kmsKeyId())
                .throughput(source.throughput())
                .volumeType(source.volumeType())
                .build();
    }
}
