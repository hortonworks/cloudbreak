package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VersionComparatorTest {

    private VersionComparator underTest;

    @BeforeEach
    public void setup() {
        underTest = new VersionComparator();
    }

    @Test
    public void testEquals() {
        assertEquals(0L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-770"));
    }

    @Test
    public void testGreater() {
        assertEquals(1L, underTest.compare(() -> "2.4.0.0-880", () -> "2.4.0.0-770"));
        assertEquals(1L, underTest.compare(() -> "2.4.0.0-1000", () -> "2.4.0.0-770"));
        assertEquals(1L, underTest.compare(() -> "2.5.0.0-1000", () -> "2.4.0.0-1000"));
        assertEquals(1L, underTest.compare(() -> "2.15.0.0-1000", () -> "2.5.0.0-1000"));
    }

    @Test
    public void testGreaterNonEqualLength() {
        assertEquals(1L, underTest.compare(() -> "2.4.0.0", () -> "2.4.0.0-770"));
        assertEquals(1L, underTest.compare(() -> "2.5.0.0", () -> "2.5.0.0-770"));
        assertEquals(1L, underTest.compare(() -> "7.2.9.1", () -> "7.2.9-1000"));
    }

    @Test
    public void testSmaller() {
        assertEquals(-1L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-880"));
        assertEquals(-1L, underTest.compare(() -> "2.4.0.0-770", () -> "2.4.0.0-1000"));
        assertEquals(-1L, underTest.compare(() -> "2.4.0.0-1000", () -> "2.5.0.0-1000"));
        assertEquals(-1L, underTest.compare(() -> "2.5.0.0-1000", () -> "2.15.0.0-1000"));
        assertEquals(-1L, underTest.compare(() -> "7.2.9", () -> "7.2.9.1"));
        assertEquals(-1L, underTest.compare(() -> "7.2.9-1000", () -> "7.2.9.1"));
        assertEquals(-1L, underTest.compare(() -> "7.2.9-1", () -> "7.2.9.1-200"));
        assertEquals(-1L, underTest.compare(() -> "7.2.9", () -> "7.2.9.1-1000"));
    }

    @Test
    public void testNull() {
        assertEquals(IllegalArgumentException.class, assertThrows(RuntimeException.class,
                () -> underTest.compare(() -> null, () -> null)).getClass());
        assertEquals(IllegalArgumentException.class, assertThrows(RuntimeException.class,
                () -> underTest.compare(() -> null, () -> "7.2.9")).getClass());
        assertEquals(IllegalArgumentException.class, assertThrows(RuntimeException.class,
                () -> underTest.compare(() -> "7.2.9.1-1000", () -> null)).getClass());
        assertEquals(IllegalArgumentException.class, assertThrows(RuntimeException.class,
                () -> underTest.compare(null, () -> "7.2.9")).getClass());
        assertEquals(IllegalArgumentException.class, assertThrows(RuntimeException.class,
                () -> underTest.compare(() -> "7.2.9", null)).getClass());
    }

    @Test
    public void compareCloudbreakVersions() {
        VersionComparator comparator = new VersionComparator();

        assertEquals(1L, comparator.compare(() -> "2.0.0-rc.1", () -> "2.0.0-dev.1"), "dev major desc");
        assertEquals(1L, comparator.compare(() -> "2.0.0", () -> "1.0.0"), "major desc");
        assertEquals(1L, comparator.compare(() -> "2.1.0", () -> "2.0.0"), "minor desc");
        assertEquals(1L, comparator.compare(() -> "2.1.1", () -> "2.1.0"), "patch desc");
        assertEquals(0L, comparator.compare(() -> "2.0.0", () -> "2.0.0"), "equals");
        assertEquals(-1L, comparator.compare(() -> "1.0.0", () -> "2.0.0"), "major asc");
        assertEquals(-1L, comparator.compare(() -> "2.0.0", () -> "2.1.0"), "minor asc");
        assertEquals(-1L, comparator.compare(() -> "2.1.0", () -> "2.1.1"), "patch asc");

        assertEquals(1L, comparator.compare(() -> "2.0.0-dev.1", () -> "1.0.0-dev.1"), "dev major desc");
        assertEquals(1L, comparator.compare(() -> "2.1.0-dev.1", () -> "2.0.0-dev.1"), "dev minor desc");
        assertEquals(1L, comparator.compare(() -> "2.1.1-dev.1", () -> "2.1.0-dev.1"), "dev patch desc");
        assertEquals(1L, comparator.compare(() -> "2.1.1-dev.2", () -> "2.1.1-dev.1"), "dev desc");
        assertEquals(0L, comparator.compare(() -> "2.0.0-dev.1", () -> "2.0.0-dev.1"), "dev equals");
        assertEquals(-1L, comparator.compare(() -> "1.0.0-dev.1", () -> "2.0.0-dev.1"), "dev major asc");
        assertEquals(-1L, comparator.compare(() -> "2.0.0-dev.1", () -> "2.1.0-dev.1"), "dev minor asc");
        assertEquals(-1L, comparator.compare(() -> "2.1.0-dev.1", () -> "2.1.1-dev.1"), "dev patch asc");
        assertEquals(-1L, comparator.compare(() -> "2.1.1-dev.1", () -> "2.1.1-dev.2"), "dev asc");
    }

    @Test
    public void testSmallerNonEqualLength() {
        assertEquals(-1, underTest.compare(() -> "2.4.0.0", () -> "2.5.0.0-770"));
    }

    @Test
    public void testComparingNewREVersioning() {
        assertEquals(-1L, underTest.compare(() -> "2.1.1-dev.1", () -> "2.1.1-b3"), "dev vs new re versioning asc");
        assertEquals(1L, underTest.compare(() -> "2.1.2-dev.13", () -> "2.1.1-b3"), "dev vs new re versioning desc");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-dev.13", () -> "2.1.1-b3"), "dev vs new re versioning desc build number");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-dev.3", () -> "2.1.1-b3"), "dev vs new re versioning equals");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.1.1-b3"), "rc vs new re versioning asc");
        assertEquals(1L, underTest.compare(() -> "2.1.2-rc.13", () -> "2.1.1-b3"), "rc vs new re versioning desc");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-rc.13", () -> "2.1.1-b3"), "rc vs new re versioning desc build number");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-rc.3", () -> "2.1.1-b3"), "rc vs new re versioning equals");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.3.1"), "released vs new re versioning asc");
        assertEquals(1L, underTest.compare(() -> "2.1.2-b13", () -> "2.1.1"), "released vs new re versioning desc");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-b2", () -> "2.1.1-b3"), "re versioning asc");
        assertEquals(1L, underTest.compare(() -> "2.1.1-b12", () -> "2.1.1-b3"), "re versioning desc");
        assertEquals(-1L, underTest.compare(() -> "2.1.2-b13", () -> "unspecified"), "unspecified vs new re versioning");

        assertEquals(-1L, underTest.compare(() -> "2.1.1-dev.1", () -> "2.2.1-b3"), "dev vs new re versioning asc");
        assertEquals(-1L, underTest.compare(() -> "2.1.1-rc.1", () -> "2.2.1-b3"), "rc vs new re versioning asc");
    }

}
