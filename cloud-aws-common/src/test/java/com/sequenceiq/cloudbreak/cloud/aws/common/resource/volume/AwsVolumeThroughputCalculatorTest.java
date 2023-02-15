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
public class AwsVolumeThroughputCalculatorTest {

    private AwsVolumeThroughputCalculator underTest = new AwsVolumeThroughputCalculator();

    @Test
    void testNotGp3() {
        assertNull(underTest.getThroughput("gp2", 1000));
        assertNull(underTest.getThroughput("st1", 1000));
    }

    @Test
    void testGp3Throughput() {
        assertEquals(125, underTest.getThroughput("gp3", 30));
        assertEquals(125, underTest.getThroughput("gp3", 100));
        assertEquals(250, underTest.getThroughput("gp3", 500));
        assertEquals(250, underTest.getThroughput("gp3", 1000));
        assertEquals(250, underTest.getThroughput("gp3", 2000));
        assertEquals(250, underTest.getThroughput("gp3", 6000));
    }

    @Test
    void testThatEbsBlockDeviceDefaultsToTheSameAsgetThroughput() {

        for (AwsDiskType volumeType : Arrays.stream(AwsDiskType.values()).filter(e -> e != Gp3).collect(Collectors.toList())) {

            EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
                    .deleteOnTermination(true)
                    .volumeType(volumeType.value())
                    .build();

            // This will fail only if AWS decides that the default throughput for a volume type is not the same as the calculated default throughput,
            // default is null at the moment
            assertEquals(ebsBlockDevice.throughput(), underTest.getThroughput(volumeType.value(), 6000));
        }
    }
}