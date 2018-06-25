package com.sequenceiq.cloudbreak.cloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.Versioned;

public class VersionComparatorTest {

    private VersionComparator underTest;

    @Before
    public void setup() {
        underTest = new VersionComparator();
    }

    @Test
    public void testEquals() {
        Assert.assertEquals(0L, underTest.compare(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770")));
    }

    @Test
    public void testGreater() {
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.4.0.0-880"), new VersionString("2.4.0.0-770")));
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.4.0.0-1000"), new VersionString("2.4.0.0-770")));
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.5.0.0-1000"), new VersionString("2.4.0.0-1000")));
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.15.0.0-1000"), new VersionString("2.5.0.0-1000")));
    }

    @Test
    public void testGreaterNonEqualLength() {
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.4.0.0"), new VersionString("2.4.0.0-770")));
        Assert.assertEquals(1L, underTest.compare(new VersionString("2.5.0.0"), new VersionString("2.5.0.0-770")));
    }

    @Test
    public void testSmaller() {
        Assert.assertEquals(-1L, underTest.compare(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-880")));
        Assert.assertEquals(-1L, underTest.compare(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-1000")));
        Assert.assertEquals(-1L, underTest.compare(new VersionString("2.4.0.0-1000"), new VersionString("2.5.0.0-1000")));
        Assert.assertEquals(-1L, underTest.compare(new VersionString("2.5.0.0-1000"), new VersionString("2.15.0.0-1000")));
    }

    @Test
    public void testSmallerNonEqualLength() {
        Assert.assertEquals(-1L, underTest.compare(new VersionString("2.4.0.0"), new VersionString("2.5.0.0-770")));
    }

    private static class VersionString implements Versioned {

        private final String version;

        private VersionString(String version) {
            this.version = version;
        }

        @Override
        public String getVersion() {
            return version;
        }
    }

}
