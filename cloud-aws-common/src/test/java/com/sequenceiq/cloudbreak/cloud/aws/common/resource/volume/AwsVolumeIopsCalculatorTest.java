package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import static com.sequenceiq.common.model.AwsDiskType.Gp3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest(name = "{0} GB volume should have {1} IOPS")
    @CsvSource({
            // Small volumes: use GP3 baseline (3000 IOPS)
            // GP2 would give 100 (min), but GP3 baseline is better
            "1, 3000",
            // GP2 would give 100 (min), but GP3 baseline is better
            "10, 3000",
            // GP2 would give 99, rounded to 100 (min), but GP3 baseline is better
            "33, 3000",
            // GP2 would give 300, but GP3 baseline is better
            "100, 3000",
            // GP2 would give 1500, but GP3 baseline is better
            "500, 3000",
            // GP2 gives 3000, equals GP3 baseline
            "1000, 3000",
            // Medium volumes: calculate based on GP2 formula
            // GP2 gives 4500 IOPS
            "1500, 4500",
            // GP2 gives 6000 IOPS
            "2000, 6000",
            // GP2 gives 9000 IOPS
            "3000, 9000",
            // GP2 gives 12000 IOPS
            "4000, 12000",
            // GP2 gives 15000 IOPS
            "5000, 15000",
            // GP2 gives 15999 IOPS (just under max)
            "5333, 15999",
            // Large volumes: cap at GP2 max (16000 IOPS)
            // GP2 would give 16002, capped at 16000
            "5334, 16000",
            // GP2 would give 18000, capped at 16000
            "6000, 16000",
            // GP2 would give 30000, capped at 16000
            "10000, 16000",
            // GP2 would give 48000, capped at 16000
            "16000, 16000"
    })
    void testCalculateEquivalentIops(int volumeSizeGb, int expectedIops) {
        int actualIops = underTest.getEquivalentGp3IopsForGp2Volume(volumeSizeGb);
        assertEquals(expectedIops, actualIops,
                String.format("Volume size %d GB should yield %d IOPS", volumeSizeGb, expectedIops));
    }

    @Test
    void testSmallVolumeUsesGp3Baseline() {
        // Volumes under 1000 GB should get at least 3000 IOPS (GP3 baseline)
        // even if GP2 would give less
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(10));
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(100));
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(999));
    }

    @Test
    void testMediumVolumeUsesGp2Calculation() {
        // Volumes that would get more than 3000 IOPS from GP2 should match that
        // GP2 formula: size * 3 IOPS/GB
        // 1001 * 3 = 3003
        assertEquals(3003, underTest.getEquivalentGp3IopsForGp2Volume(1001));
        // 2000 * 3 = 6000
        assertEquals(6000, underTest.getEquivalentGp3IopsForGp2Volume(2000));
        // 4000 * 3 = 12000
        assertEquals(12000, underTest.getEquivalentGp3IopsForGp2Volume(4000));
    }

    @Test
    void testLargeVolumeCappedAtMax() {
        // Volumes larger than 5333 GB would exceed 16000 IOPS limit
        // Should be capped at 16000
        assertEquals(16000, underTest.getEquivalentGp3IopsForGp2Volume(5334));
        assertEquals(16000, underTest.getEquivalentGp3IopsForGp2Volume(10000));
        assertEquals(16000, underTest.getEquivalentGp3IopsForGp2Volume(100000));
    }

    @Test
    void testBoundaryConditions() {
        // Test exactly at the transition points

        // At 1000 GB: GP2 gives exactly 3000 IOPS, which equals GP3 baseline
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(1000));

        // At 5333 GB: GP2 gives 15999 IOPS (just under max)
        assertEquals(15999, underTest.getEquivalentGp3IopsForGp2Volume(5333));

        // At 5334 GB: GP2 would give 16002 IOPS, but capped at 16000
        assertEquals(16000, underTest.getEquivalentGp3IopsForGp2Volume(5334));
    }

    @Test
    void testMinimumVolumeSize() {
        // Even a 1 GB volume should get reasonable IOPS (GP3 baseline)
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(1));
    }

    @Test
    void testTypicalUseCases() {
        // Test some typical real-world volume sizes
        // Small boot volume
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(50));
        // Medium boot volume
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(100));
        // Large boot volume
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(500));
        // 1 TB data volume
        assertEquals(3000, underTest.getEquivalentGp3IopsForGp2Volume(1000));
        // 2 TB data volume
        assertEquals(6000, underTest.getEquivalentGp3IopsForGp2Volume(2000));
        // 5 TB data volume
        assertEquals(15000, underTest.getEquivalentGp3IopsForGp2Volume(5000));
    }

}