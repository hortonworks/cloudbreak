package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAINTENANCE;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAJOR;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MINOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionPrefixTest {

    private VersionPrefix underTest;

    @BeforeEach
    void setup() {
        underTest = new VersionPrefix();
    }

    @Test
    void testEquals() {
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 1));
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 2));
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 3));
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 4));

        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MAJOR));
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MINOR));
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MAINTENANCE));
    }

    @Test
    void testDifferentPrefix() {
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 1));
        assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 2));
        assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 3));

        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MAJOR));
        assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MINOR));
        assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MAINTENANCE));

        assertTrue(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MAJOR));
        assertTrue(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MINOR));
        assertFalse(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MAINTENANCE));
    }

    @Test
    void testTooLong() {
        assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 5));
        assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 6));
    }
}
