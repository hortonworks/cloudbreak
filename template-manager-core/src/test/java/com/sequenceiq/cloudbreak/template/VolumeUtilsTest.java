package com.sequenceiq.cloudbreak.template;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VolumeUtilsTest {

    @Test
    public void testBuildVolumePathStringWithZeroDisk() {
        String directories = VolumeUtils.buildVolumePathString(0, "test");

        assertEquals("", directories);
    }

    @Test
    public void testBuildVolumePathStringWithOneDisk() {
        String directories = VolumeUtils.buildVolumePathString(1, "test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    public void testBuildVolumePathStringWithManyDisks() {
        String directories = VolumeUtils.buildVolumePathString(3, "test");

        assertEquals("/hadoopfs/fs1/test,/hadoopfs/fs2/test,/hadoopfs/fs3/test", directories);
    }

    @Test
    public void testGetLogVolume() {
        String directories = VolumeUtils.getLogVolume("test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    public void testBuildVolumePathStringZeroVolumesHandledWithZeroDisk() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(0, "test");

        assertEquals("/hadoopfs/root1/test", directories);
    }

    @Test
    public void testBuildVolumePathStringZeroVolumesHandledWithOneDisk() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(1, "test");

        assertEquals("/hadoopfs/fs1/test", directories);
    }

    @Test
    public void testBuildVolumePathStringZeroVolumesHandledWithManyDisks() {
        String directories = VolumeUtils.buildVolumePathStringZeroVolumeHandled(3, "test");

        assertEquals("/hadoopfs/fs1/test,/hadoopfs/fs2/test,/hadoopfs/fs3/test", directories);
    }
}
