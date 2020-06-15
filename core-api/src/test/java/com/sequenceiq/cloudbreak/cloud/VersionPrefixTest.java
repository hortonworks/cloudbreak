package com.sequenceiq.cloudbreak.cloud;

import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MINOR;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAJOR;
import static com.sequenceiq.cloudbreak.cloud.PrefixMatchLength.MAINTENANCE;

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

        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MAJOR));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MINOR));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), MAINTENANCE));
    }

    @Test
    public void testDifferentPrefix() {
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 1));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 2));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), 3));

        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MAJOR));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MINOR));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.5.0.0-770"), MAINTENANCE));

        Assert.assertTrue(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MAJOR));
        Assert.assertTrue(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MINOR));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("3000.2"), new VersionString("3000.2"), MAINTENANCE));
    }

    @Test
    public void testTooLong() {
        Assert.assertTrue(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 5));
        Assert.assertFalse(underTest.prefixMatch(new VersionString("2.4.0.0-770"), new VersionString("2.4.0.0-770"), 6));
    }
}
