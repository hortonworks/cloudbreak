package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDeviceRequest;

class LaunchTemplateEbsBlockDeviceToRequestConverterTest {

    private LaunchTemplateEbsBlockDeviceToRequestConverter underTest = new LaunchTemplateEbsBlockDeviceToRequestConverter();

    @Test
    void testConvert() {
        LaunchTemplateEbsBlockDevice source = LaunchTemplateEbsBlockDevice.builder()
                .deleteOnTermination(Boolean.TRUE)
                .volumeSize(100)
                .encrypted(Boolean.TRUE)
                .iops(10000)
                .kmsKeyId("fda")
                .throughput(12312)
                .volumeType("gp5000")
                .build();

        LaunchTemplateEbsBlockDeviceRequest result = underTest.convert(source);

        assertEquals(source.deleteOnTermination(), result.deleteOnTermination());
        assertEquals(source.volumeSize(), result.volumeSize());
        assertEquals(source.encrypted(), result.encrypted());
        assertEquals(source.iops(), result.iops());
        assertEquals(source.kmsKeyId(), result.kmsKeyId());
        assertEquals(source.throughput(), result.throughput());
        assertEquals(source.volumeType(), result.volumeType());
    }
}