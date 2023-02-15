package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.common.model.AwsDiskType.Gp3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;

/**
 * Test cases are from: https://aws.amazon.com/blogs/storage/migrate-your-amazon-ebs-volumes-from-gp2-to-gp3-and-save-up-to-20-on-costs/
 */
public class AwsVolumeIopsCalculatorTest {

    private AwsVolumeIopsCalculator underTest = new AwsVolumeIopsCalculator();

    @Test
    void testNotGp3() {
        assertNull(underTest.getIops("gp2", 1000));
        assertNull(underTest.getIops("st1", 1000));
    }

    @Test
    void testGp3Iops() {
        assertEquals(3000, underTest.getIops("gp3", 30));
        assertEquals(3000, underTest.getIops("gp3", 100));
        assertEquals(3000, underTest.getIops("gp3", 500));
        assertEquals(3000, underTest.getIops("gp3", 1000));
        assertEquals(6000, underTest.getIops("gp3", 2000));
        assertEquals(16000, underTest.getIops("gp3", 6000));
    }

    @Test
    void testThatEbsBlockDeviceDefaultsToTheSameAsGetIops() {

        for (AwsDiskType volumeType : Arrays.stream(AwsDiskType.values()).filter(e -> e != Gp3).collect(Collectors.toList())) {

            EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
                    .deleteOnTermination(true)
                    .volumeType(volumeType.value())
                    .build();

            // This will fail only if AWS decides that the default IOPS for a volume type is not the same as the caclulated default IOPS,
            // default is null at the moment
            assertEquals(ebsBlockDevice.iops(), underTest.getIops(volumeType.value(), 6000));
        }
    }
}