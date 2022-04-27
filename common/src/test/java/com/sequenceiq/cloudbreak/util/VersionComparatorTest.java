package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VersionComparatorTest {

    private VersionComparator underTest;

    @Before
    public void setup() {
        underTest = new VersionComparator();
    }

    @Test
    public void testEquals() {
        Assert.assertEquals(0L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-770"));
    }

    @Test
    public void testGreater() {
        Assert.assertEquals(1L, underTest.compare(() -> "2.4.0.0-880", () -> "2.4.0.0-770"));
        Assert.assertEquals(1L, underTest.compare(() -> "2.4.0.0-1000", () -> "2.4.0.0-770"));
        Assert.assertEquals(1L, underTest.compare(() -> "2.5.0.0-1000", () -> "2.4.0.0-1000"));
        Assert.assertEquals(1L, underTest.compare(() -> "2.15.0.0-1000", () -> "2.5.0.0-1000"));
    }

    @Test
    public void testGreaterNonEqualLength() {
        Assert.assertEquals(1L, underTest.compare(() -> "2.4.0.0", () -> "2.4.0.0-770"));
        Assert.assertEquals(1L, underTest.compare(() -> "2.5.0.0", () -> "2.5.0.0-770"));
        Assert.assertEquals(1L, underTest.compare(() -> "7.2.9.1", () -> "7.2.9-1000"));
    }

    @Test
    public void testSmaller() {
        Assert.assertEquals(-1L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-880"));
        Assert.assertEquals(-1L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-1000"));
        Assert.assertEquals(-1L, underTest.compare(() -> "2.4.0.0-1000", () -> "2.5.0.0-1000"));
        Assert.assertEquals(-1L, underTest.compare(() -> "2.5.0.0-1000", () -> "2.15.0.0-1000"));
        Assert.assertEquals(-1L, underTest.compare(() -> "7.2.9", () -> "7.2.9.1"));
        Assert.assertEquals(-1L, underTest.compare(() -> "7.2.9-1000", () -> "7.2.9.1"));
        Assert.assertEquals(-1L, underTest.compare(() -> "7.2.9-1", () -> "7.2.9.1-200"));
        Assert.assertEquals(-1L, underTest.compare(() -> "7.2.9", () -> "7.2.9.1-1000"));
    }

    @Test
    public void compareCloudbreakVersions() {
        VersionComparator comparator = new VersionComparator();

        assertEquals("dev major desc", 1L, comparator.compare(() -> "2.0.0-rc.1", () -> "2.0.0-dev.1"));
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
        Assert.assertEquals(-1, underTest.compare(() -> "2.4.0.0", () -> "2.5.0.0-770"));
    }

    @Test
    public void testComparingNewREVersioning() {
        assertEquals("dev vs new re versioning asc", -1L, underTest.compare(() -> "2.1.1-dev.1", () -> "2.1.1-b3"));
        assertEquals("dev vs new re versioning desc", 1L, underTest.compare(() -> "2.1.2-dev.13", () -> "2.1.1-b3"));
        assertEquals("dev vs new re versioning desc build number", -1L, underTest.compare(() -> "2.1.1-dev.13", () -> "2.1.1-b3"));
        assertEquals("dev vs new re versioning equals", -1L, underTest.compare(() -> "2.1.1-dev.3", () -> "2.1.1-b3"));
        assertEquals("rc vs new re versioning asc", -1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.1.1-b3"));
        assertEquals("rc vs new re versioning desc", 1L, underTest.compare(() -> "2.1.2-rc.13", () -> "2.1.1-b3"));
        assertEquals("rc vs new re versioning desc build number", -1L, underTest.compare(() -> "2.1.1-rc.13", () -> "2.1.1-b3"));
        assertEquals("rc vs new re versioning equals", -1L, underTest.compare(() -> "2.1.1-rc.3", () -> "2.1.1-b3"));
        assertEquals("released vs new re versioning asc", -1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.3.1"));
        assertEquals("released vs new re versioning desc", 1L, underTest.compare(() -> "2.1.2-b13", () -> "2.1.1"));
        assertEquals("re versioning asc", -1L, underTest.compare(() -> "2.1.1-b2", () -> "2.1.1-b3"));
        assertEquals("re versioning desc", 1L, underTest.compare(() -> "2.1.1-b12", () -> "2.1.1-b3"));
        assertEquals("unspecified vs new re versioning", -1L, underTest.compare(() -> "2.1.2-b13", () -> "unspecified"));

        assertEquals("dev vs new re versioning asc", -1L, underTest.compare(() -> "2.1.1-dev.1", () -> "2.2.1-b3"));
        assertEquals("rc vs new re versioning asc", -1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.2.1-b3"));
    }

}
