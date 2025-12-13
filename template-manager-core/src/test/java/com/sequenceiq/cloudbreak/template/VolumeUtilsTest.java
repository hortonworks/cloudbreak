package com.sequenceiq.cloudbreak.template;

import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VolumeUtilsTest {

    @Test
    void testBuildVolumePathStringWithZeroDisk() {
        String directories = VolumeUtils.buildVolumePathString(0, "test");

        assertEquals("", directories);
    }

    @Test
    void testBuildVolumePathStringWithOneDisk() {
        String directories = VolumeUtils.buildVolumePathString(1, "test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    void testBuildVolumePathStringWithManyDisks() {
        String directories = VolumeUtils.buildVolumePathString(3, "test");

        assertEquals("/hadoopfs/fs1/test,/hadoopfs/fs2/test,/hadoopfs/fs3/test", directories);
    }

    @Test
    void testGetLogVolume() {
        String directories = VolumeUtils.getLogVolume("test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    void testBuildVolumePathStringZeroVolumesHandledWithZeroDisk() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(0, "test");

        assertEquals("/hadoopfs/root1/test", directories);
    }

    @Test
    void testBuildVolumePathStringZeroVolumesHandledWithOneDisk() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(1, "test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    void testBuildVolumePathStringZeroVolumesHandledWithManyDisks() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(3, "test");

        assertEquals("/hadoopfs/fs1/test,/hadoopfs/fs2/test,/hadoopfs/fs3/test", directories);
    }

    @Test
    void buildSingleVolumePathWithZeroDisk() {
        assertEquals("/hadoopfs/root1/test", buildSingleVolumePath(0, "test"));
    }

    @Test
    void buildSingleVolumePathWithOneDisk() {
        assertEquals("/hadoopfs/fs1/test", buildSingleVolumePath(1, "test"));
    }

    @Test
    void buildSingleVolumePathWithManyDisks() {
        assertEquals("/hadoopfs/fs1/test", buildSingleVolumePath(3, "test"));
    }

    @Test
    void testBuildSingleVolumePathWithVolumeId() {
        assertEquals("/hadoopfs/fs5/test", buildSingleVolumePath(5, 10, "test"));
    }

    @Test
    void testBuildSingleVolumePathWithVolumeIdWhenThereAreNotEnoughVolumes() {
        assertEquals("/hadoopfs/fs3/test", buildSingleVolumePath(5, 3, "test"));
    }

    @Test
    void testBuildSingleVolumePathWithVolumeIdWhenThereAreNoVolumes() {
        assertEquals("/hadoopfs/root1/test", buildSingleVolumePath(5, 0, "test"));
    }
}
