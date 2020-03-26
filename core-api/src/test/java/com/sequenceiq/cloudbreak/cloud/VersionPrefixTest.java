package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAINTENANCE;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MINOR;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.PATCH;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VersionPrefixTest {

    private VersionPrefix underTest;

    @Before
    public void setup() {
        underTest = new VersionPrefix();
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 1));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 2));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 3));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 4));

        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MINOR));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MAINTENANCE));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), PATCH));
    }

    @Test
    public void testDiffernetPrefix() {
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 1));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 2));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 3));

        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MINOR));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MAINTENANCE));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), PATCH));
    }

    @Test
    public void testTooLong() {
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 5));
    }
}
