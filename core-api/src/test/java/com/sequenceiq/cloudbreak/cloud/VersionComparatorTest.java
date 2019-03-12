package com.sequenceiq.cloudbreak.cloud;

import static org.junit.Assert.assertEquals;

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
    public void compareCloudbreakVersions() {
        VersionComparator comparator = new VersionComparator();

        assertEquals("major desc", 1L, comparator.compare(() -> "2.0.0", () -> "1.0.0"));
        assertEquals("minor desc", 1L, comparator.compare(() -> "2.1.0", () -> "2.0.0"));
        assertEquals("patch desc", 1L, comparator.compare(() -> "2.1.1", () -> "2.1.0"));
        assertEquals("equals", 0L, comparator.compare(() -> "2.0.0", () -> "2.0.0"));
        assertEquals("major asc", -1L, comparator.compare(() -> "1.0.0", () -> "2.0.0"));
        assertEquals("minor asc", -1L, comparator.compare(() -> "2.0.0", () -> "2.1.0"));
        assertEquals("patch asc", -1L, comparator.compare(() -> "2.1.0", () -> "2.1.1"));

        assertEquals("dev major desc", 1L, comparator.compare(() -> "2.0.0-dev.1", () -> "1.0.0-dev.1"));
        assertEquals("dev minor desc", 1L, comparator.compare(() -> "2.1.0-dev.1", () -> "2.0.0-dev.1"));
        assertEquals("dev patch desc", 1L, comparator.compare(() -> "2.1.1-dev.1", () -> "2.1.0-dev.1"));
        assertEquals("dev desc", 1L, comparator.compare(() -> "2.1.1-dev.2", () -> "2.1.1-dev.1"));
        assertEquals("dev equals", 0L, comparator.compare(() -> "2.0.0-dev.1", () -> "2.0.0-dev.1"));
        assertEquals("dev major asc", -1L, comparator.compare(() -> "1.0.0-dev.1", () -> "2.0.0-dev.1"));
        assertEquals("dev minor asc", -1L, comparator.compare(() -> "2.0.0-dev.1", () -> "2.1.0-dev.1"));
        assertEquals("dev patch asc", -1L, comparator.compare(() -> "2.1.0-dev.1", () -> "2.1.1-dev.1"));
        assertEquals("dev asc", -1L, comparator.compare(() -> "2.1.1-dev.1", () -> "2.1.1-dev.2"));
    }

    @Test
    public void testSmallerNonEqualLength() {
        Assert.assertEquals(-1, underTest.compare(new VersionString("2.4.0.0"), new VersionString("2.5.0.0-770")));
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
